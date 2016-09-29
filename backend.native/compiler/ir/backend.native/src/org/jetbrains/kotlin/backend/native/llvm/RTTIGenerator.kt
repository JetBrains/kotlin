package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import java.util.zip.CRC32

private fun crc32(str: String): Long {
    val c = CRC32()
    c.update(str.toByteArray())
    return c.value
}

private fun getLLVMType(field: IrField): LLVMOpaqueType {
    val type = field.descriptor.returnType!!
    return when {
        KotlinBuiltIns.isBoolean(type) || KotlinBuiltIns.isByte(type) -> LLVMInt8Type()
        KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isChar(type) -> LLVMInt16Type()
        KotlinBuiltIns.isInt(type) -> LLVMInt32Type()
        KotlinBuiltIns.isLong(type) -> LLVMInt64Type()
        else -> throw NotImplementedError()
    }!!
}

class RTTIGenerator(val module: LLVMOpaqueModule, val runtime: Runtime): IrElementVisitorVoid {

    /**
     * Represents the value which can be emitted as bitcode const value
     */
    private inner abstract class CompileTimeValue {

        abstract fun getLlvmValue(): LLVMOpaqueValue?

        fun getLlvmType(): LLVMOpaqueType? {
            return LLVMTypeOf(getLlvmValue())
        }

    }

    private inner class ConstArray(val elemType: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue() {

        constructor(type: LLVMOpaqueType?, vararg elements: CompileTimeValue) : this(type, elements.toList())

        override fun getLlvmValue(): LLVMOpaqueValue? {
            val values = elements.map { it.getLlvmValue() }.toTypedArray()
            val valuesNativeArrayPtr = arena.allocNativeArrayOf(LLVMOpaqueValue, *values)[0]

            return LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)
        }
    }

    private inner open class Struct(val type: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue() {

        constructor(type: LLVMOpaqueType?, vararg elements: CompileTimeValue) : this(type, elements.toList())

        override fun getLlvmValue(): LLVMOpaqueValue? {
            val values = elements.map { it.getLlvmValue() }.toTypedArray()
            val valuesNativeArrayPtr = arena.allocNativeArrayOf(LLVMOpaqueValue, *values)[0]
            return LLVMConstNamedStruct(type, valuesNativeArrayPtr, values.size)
        }
    }

    private inner class Int32(val value: Int) : CompileTimeValue() {
        override fun getLlvmValue() = LLVMConstInt(LLVMInt32Type(), value.toLong(), 1)
    }

    private inner class Int64(val value: Long) : CompileTimeValue() {
        override fun getLlvmValue() = LLVMConstInt(LLVMInt64Type(), value, 1)
    }

    private inner class Zero(val type: LLVMOpaqueType?) : CompileTimeValue() {
        override fun getLlvmValue() = LLVMConstNull(type)
    }

    private fun compileTimeValue(value: LLVMOpaqueValue?) = object : CompileTimeValue() {
        override fun getLlvmValue() = value
    }

    private val int32Type = LLVMInt32Type()

    private fun pointer(type: LLVMOpaqueType?) = LLVMPointerType(type, 0) // TODO: why 0?


    private inner class FieldTableRecord(val nameSignature: Long, val fieldOffset: Int) :
            Struct(runtime.fieldTableRecordType, Int64(nameSignature), Int32(fieldOffset))

    private inner class TypeInfo(val name: Long, val size: Int, val superType: CompileTimeValue, val fields: CompileTimeValue,
                                          val fieldsCount: Int) :
            Struct(
                    runtime.typeInfoType,
                    Int64(name),
                    Int32(size),

                    superType,

                    Zero(pointer(int32Type)),
                    Int32(0),

                    Zero(pointer(pointer(runtime.typeInfoType))),
                    Int32(0),

                    Zero(pointer(runtime.methodTableRecordType)),
                    Int32(0),

                    fields,
                    Int32(fieldsCount)
            )

    private val builder = LLVMCreateBuilder() // TODO: dispose
    private val arena = Arena() // TODO: dispose

    private fun addGlobalConst(name: String, value: CompileTimeValue): LLVMOpaqueValue? {
        val global = LLVMAddGlobal(module, value.getLlvmType(), name)
        LLVMSetInitializer(global, value.getLlvmValue())
        return global
    }

    private fun getGlobalArrayPtr(array: LLVMOpaqueValue?): LLVMOpaqueValue? {
        val indices = longArrayOf(0, 0).map { LLVMConstInt(LLVMInt32Type(), it, 0) }.toTypedArray()
        val indicesNativeArrayPtr = arena.allocNativeArrayOf(LLVMOpaqueValue, *indices)[0]

        return LLVMBuildGEP(builder, array, indicesNativeArrayPtr, indices.size, "")
    }

    private fun typeInfoFor(classDesc: ClassDescriptor): LLVMOpaqueValue? {
        val globalName = classDesc.name.toString() + "_type" // FIXME: FQDN
        return LLVMGetNamedGlobal(module, globalName) ?: LLVMAddGlobal(module, runtime.typeInfoType, globalName)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        val className = declaration.descriptor.name // FIXME: FQDN
        val fields = declaration.declarations.filterIsInstance<IrProperty>().mapNotNull { it.backingField }
        val fieldTypes = fields.map { getLLVMType(it) }.toTypedArray()
        val classType = LLVMStructCreateNamed(LLVMGetModuleContext(module), "class." + className)
        LLVMStructSetBody(classType, mallocNativeArrayOf(LLVMOpaqueType, *fieldTypes)[0], fieldTypes.size, 0)

        val name = crc32(className.toString())
        val size = LLVMStoreSizeOfType(runtime.targetData, classType).toInt()
        val superType = compileTimeValue(typeInfoFor(declaration.descriptor.getSuperClassOrAny()))

        val fieldTableRecords = fields.mapIndexed { index, field ->
            val nameSignature = crc32(field.descriptor.name.toString()) // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(runtime.targetData, classType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature }

        val fieldsArray = ConstArray(runtime.fieldTableRecordType, fieldTableRecords)
        val fieldsGlobal = addGlobalConst("${className}_fields", fieldsArray)
        val fieldsPtr = compileTimeValue(getGlobalArrayPtr(fieldsGlobal))

        val typeInfo = TypeInfo(name, size, superType, fieldsPtr, fieldTableRecords.size)
        val typeInfoGlobal = typeInfoFor(declaration.descriptor)
        LLVMSetInitializer(typeInfoGlobal, typeInfo.getLlvmValue())
    }

}