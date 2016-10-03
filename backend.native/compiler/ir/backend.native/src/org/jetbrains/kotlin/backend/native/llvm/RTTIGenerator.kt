package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.mallocNativeArrayOf
import llvm.*
import org.jetbrains.kotlin.backend.native.implementation
import org.jetbrains.kotlin.backend.native.implementedInterfaces
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny


internal class RTTIGenerator(override val context: Context) : ContextUtils {

    val runtime: Runtime
        get() = context.runtime


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

    // TODO: probably it should be moved out of this class and shared.
    private fun createStructFor(className: FqName, fields: List<PropertyDescriptor>): LLVMOpaqueType? {
        val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), "kclass:" + className)
        val fieldTypes = fields.map { getLLVMType(it.returnType!!) }.toTypedArray()
        val fieldTypesNativeArrayPtr = if (fieldTypes.size > 0) {
            mallocNativeArrayOf(LLVMOpaqueType, *fieldTypes)[0] // TODO: dispose
        } else {
            null
        }

        LLVMStructSetBody(classType, fieldTypesNativeArrayPtr, fieldTypes.size, 0)
        return classType
    }

    private fun getMethodTableEntries(classDesc: ClassDescriptor): List<FunctionDescriptor> {
        val contributedDescriptors = classDesc.unsubstitutedMemberScope.getContributedDescriptors()
         // (includes declarations from supers)

        val methods = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        return methods + getters + setters
    }

    fun generate(classDesc: ClassDescriptor) {

        val className = classDesc.fqNameSafe

        val classType = createStructFor(className, classDesc.fields)

        val name = className.nameHash

        val size = LLVMStoreSizeOfType(runtime.targetData, classType).toInt()

        val superType = classDesc.getSuperClassOrAny().llvmTypeInfoPtr

        val interfaces = classDesc.implementedInterfaces.map { it.llvmTypeInfoPtr }
        val interfacesPtr = addGlobalArray("kintf:$className", pointerType(runtime.typeInfoType), interfaces)

        val refFieldIndices = classDesc.fields.mapIndexedNotNull { index, field ->
            val type = field.returnType!!
            if (!KotlinBuiltIns.isPrimitiveType(type)) {
                index
            } else {
                null
            }
        }
        // TODO: reuse offsets obtained for 'fields' below
        val objOffsets = refFieldIndices.map { LLVMOffsetOfElement(runtime.targetData, classType, it) }
        val objOffsetsPtr = addGlobalArray("krefs:$className", int32Type, objOffsets.map { Int32(it.toInt()) })

        val fields = classDesc.fields.mapIndexed { index, field ->
            // Note: using FQ name because a class may have multiple fields with the same name due to property overriding
            val nameSignature = field.fqNameSafe.nameHash // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(runtime.targetData, classType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature }

        val fieldsPtr = addGlobalArray("kfields:$className", runtime.fieldTableRecordType, fields)

        val methods = getMethodTableEntries(classDesc).map {
            val nameSignature = it.name.nameHash // FIXME: add signature
            // TODO: compile-time resolution limits binary compatibility
            val methodEntryPoint = it.implementation.entryPointAddress
            MethodTableRecord(nameSignature, methodEntryPoint)
        }.sortedBy { it.nameSignature }

        val methodsPtr = addGlobalArray("kmethods:$className", runtime.methodTableRecordType, methods)

        val typeInfo = TypeInfo(name, size,
                                superType,
                                objOffsetsPtr, objOffsets.size,
                                interfacesPtr, interfaces.size,
                                methodsPtr, methods.size,
                                fieldsPtr, fields.size)

        val typeInfoGlobal = classDesc.llvmTypeInfoPtr.getLlvmValue() // TODO: it is a hack
        LLVMSetInitializer(typeInfoGlobal, typeInfo.getLlvmValue())
    }

}