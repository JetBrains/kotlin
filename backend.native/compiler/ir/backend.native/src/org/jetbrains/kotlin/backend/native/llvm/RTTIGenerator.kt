package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import java.util.zip.CRC32

private fun crc32(str: String): Long {
    val c = CRC32()
    c.update(str.toByteArray())
    return c.value
}

private fun crc32(name: Name) = crc32(name.toString())

private fun getKotlinType(field: IrField) = field.descriptor.returnType!!

private fun getLLVMType(field: IrField): LLVMOpaqueType {
    val type = getKotlinType(field)
    return when {
        KotlinBuiltIns.isBoolean(type) || KotlinBuiltIns.isByte(type) -> LLVMInt8Type()
        KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isChar(type) -> LLVMInt16Type()
        KotlinBuiltIns.isInt(type) -> LLVMInt32Type()
        KotlinBuiltIns.isLong(type) -> LLVMInt64Type()
        !KotlinBuiltIns.isPrimitiveType(type) -> LLVMPointerType(LLVMInt8Type(), 0)
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

    private inner class MethodTableRecord(val nameSignature: Long, val methodEntryPoint: CompileTimeValue) :
            Struct(runtime.methodTableRecordType, Int64(nameSignature), methodEntryPoint)

    private inner class TypeInfo(val name: Long, val size: Int,
                                 val superType: CompileTimeValue,
                                 val objOffsets: CompileTimeValue,
                                 val objOffsetsCount: Int,
                                 val interfaces: CompileTimeValue,
                                 val interfacesCount: Int,
                                 val methods: CompileTimeValue,
                                 val methodsCount: Int,
                                 val fields: CompileTimeValue,
                                 val fieldsCount: Int) :
            Struct(
                    runtime.typeInfoType,

                    Int64(name),
                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    methods,
                    Int32(methodsCount),

                    fields,
                    Int32(fieldsCount)
            )

    private val builder = LLVMCreateBuilder() // TODO: dispose
    private val arena = Arena() // TODO: dispose

    private fun addGlobalConst(name: String, value: CompileTimeValue): CompileTimeValue {
        val global = LLVMAddGlobal(module, value.getLlvmType(), name)
        LLVMSetInitializer(global, value.getLlvmValue())
        return compileTimeValue(global)
    }

    private fun getPtrToFirstElem(arrayPtr: CompileTimeValue): CompileTimeValue {
        val indices = longArrayOf(0, 0).map { LLVMConstInt(LLVMInt32Type(), it, 0) }.toTypedArray()
        val indicesNativeArrayPtr = arena.allocNativeArrayOf(LLVMOpaqueValue, *indices)[0]

        return compileTimeValue(LLVMBuildGEP(builder, arrayPtr.getLlvmValue(), indicesNativeArrayPtr, indices.size, ""))
    }

    private fun addGlobalArray(name: String, elemType: LLVMOpaqueType?, elements: List<CompileTimeValue>): CompileTimeValue {
        return if (elements.size > 0) {
            getPtrToFirstElem(addGlobalConst(name, ConstArray(elemType, elements)))
        } else {
            Zero(pointer(elemType))
        }
    }

    private fun typeInfoFor(classDesc: ClassDescriptor): LLVMOpaqueValue? {
        val globalName = "ktype:" + classDesc.fqNameSafe.toString()
        return LLVMGetNamedGlobal(module, globalName) ?: LLVMAddGlobal(module, runtime.typeInfoType, globalName)
    }

    private fun createStructFor(className: FqName, declaredFields: List<IrField>): LLVMOpaqueType? {
        val fieldTypes = declaredFields.map { getLLVMType(it) }.toTypedArray()
        val classType = LLVMStructCreateNamed(LLVMGetModuleContext(module), "kclass:" + className)
        val fieldTypesNativeArray = mallocNativeArrayOf(LLVMOpaqueType, *fieldTypes)
        LLVMStructSetBody(classType, fieldTypesNativeArray[0], fieldTypes.size, 0)
        return classType
    }

    private fun methodEntryPoint(function: FunctionDescriptor): CompileTimeValue {
        val globalName = "kfun:" + function.fqNameSafe.toString() // FIXME: add signature
        val functionType = LLVMFunctionType(LLVMVoidType(), null, 0, 0) // FIXME: use correct types
        val function = LLVMGetNamedFunction(module, globalName) ?: LLVMAddFunction(module, globalName, functionType)
        val result = compileTimeValue(LLVMConstBitCast(function, pointer(LLVMInt8Type())))
        return result
    }

    private fun getDeclaredFields(irClass: IrClass) = irClass.declarations.mapNotNull { (it as? IrProperty)?.backingField }

    private fun getDeclaredMethods(irClass: IrClass): List<IrFunction> {
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        val properties = irClass.declarations.filterIsInstance<IrProperty>()
        return functions + properties.mapNotNull { it.getter } + properties.mapNotNull { it.setter }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitClass(declaration: IrClass) {
        visitElement(declaration)

        val className = declaration.descriptor.fqNameSafe

        val declaredFields = getDeclaredFields(declaration)

        val classType = createStructFor(className, declaredFields)

        val name = crc32(className.toString())

        val size = LLVMStoreSizeOfType(runtime.targetData, classType).toInt()

        val superType = compileTimeValue(typeInfoFor(declaration.descriptor.getSuperClassOrAny()))

        val interfaces = declaration.descriptor.getSuperInterfaces().map { compileTimeValue(typeInfoFor(it)) }
        val interfacesPtr = addGlobalArray("kintf:$className", pointer(runtime.typeInfoType), interfaces)

        val refFieldIndices = declaredFields.mapIndexedNotNull { index, field ->
            val type = getKotlinType(field)
            if (!KotlinBuiltIns.isPrimitiveType(type)) {
                index
            } else {
                null
            }
        }
        // TODO: reuse offsets obtained for 'fields' below
        val objOffsets = refFieldIndices.map { LLVMOffsetOfElement(runtime.targetData, classType, it) }
        val objOffsetsPtr = addGlobalArray("krefs:$className", int32Type, objOffsets.map { Int32(it.toInt()) })

        // TODO: add fields from supers
        val fields = declaredFields.mapIndexed { index, field ->
            val nameSignature = crc32(field.descriptor.name) // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(runtime.targetData, classType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature }

        val fieldsPtr = addGlobalArray("kfields:$className", runtime.fieldTableRecordType, fields)

        // TODO: add methods from supers
        val methods = getDeclaredMethods(declaration).map {
            val nameSignature = crc32(it.descriptor.name) // FIXME: add signature
            val methodEntryPoint = methodEntryPoint(it.descriptor)
            MethodTableRecord(nameSignature, methodEntryPoint)
        }.sortedBy { it.nameSignature }

        val methodsPtr = addGlobalArray("kmethods:$className", runtime.methodTableRecordType, methods)

        val typeInfo = TypeInfo(name, size,
                                superType,
                                objOffsetsPtr, objOffsets.size,
                                interfacesPtr, interfaces.size,
                                methodsPtr, methods.size,
                                fieldsPtr, fields.size)

        val typeInfoGlobal = typeInfoFor(declaration.descriptor)
        LLVMSetInitializer(typeInfoGlobal, typeInfo.getLlvmValue())
    }

}