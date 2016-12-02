package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.implementation
import org.jetbrains.kotlin.backend.konan.implementedInterfaces
import org.jetbrains.kotlin.backend.konan.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny


internal class RTTIGenerator(override val context: Context) : ContextUtils {

    private inner class FieldTableRecord(val nameSignature: LocalHash, val fieldOffset: Int) :
            Struct(runtime.fieldTableRecordType, nameSignature, Int32(fieldOffset))

    private inner class MethodTableRecord(val nameSignature: LocalHash, val methodEntryPoint: ConstValue) :
            Struct(runtime.methodTableRecordType, nameSignature, methodEntryPoint)

    private inner class TypeInfo(val name: ConstValue, val size: Int,
                                 val superType: ConstValue,
                                 val objOffsets: ConstValue,
                                 val objOffsetsCount: Int,
                                 val interfaces: ConstValue,
                                 val interfacesCount: Int,
                                 val vtable: ConstValue,
                                 val methods: ConstValue,
                                 val methodsCount: Int,
                                 val fields: ConstValue,
                                 val fieldsCount: Int) :
            Struct(
                    runtime.typeInfoType,

                    name,
                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    vtable,

                    methods,
                    Int32(methodsCount),

                    fields,
                    Int32(fieldsCount)
            )

    // TODO: probably it should be moved out of this class and shared.
    private fun createStructFor(className: FqName, fields: List<PropertyDescriptor>): LLVMTypeRef? {
        val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), "kclass:" + className)
        val fieldTypes = fields.map { getLLVMType(it.returnType!!) }.toTypedArray()

        memScoped {
            val fieldTypesNativeArrayPtr = allocArrayOf(*fieldTypes)[0].ptr
            LLVMStructSetBody(classType, fieldTypesNativeArrayPtr, fieldTypes.size, 0)
        }
        return classType
    }

    // TODO: optimize
    private fun getVtableEntries(classDesc: ClassDescriptor): List<FunctionDescriptor> {
        assert (!classDesc.isInterface)

        val superVtableEntries = if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(classDesc)) {
            emptyList()
        } else {
            getVtableEntries(classDesc.getSuperClassOrAny())
        }

        val methods = classDesc.getContributedMethods() // TODO: ensure order is well-defined

        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            methods.single { OverridingUtil.overrides(it, superMethod) }
        }

        return inheritedVtableSlots + (methods - inheritedVtableSlots).filter { it.isOverridable }
    }

    private fun getMethodTableEntries(classDesc: ClassDescriptor): List<FunctionDescriptor> {
        assert (classDesc.modality != Modality.ABSTRACT)

        return classDesc.getContributedMethods().filter { it.isOverridableOrOverrides }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

    private fun ClassDescriptor.getContributedMethods(): List<FunctionDescriptor> {
        val contributedDescriptors = unsubstitutedMemberScope.getContributedDescriptors()
        // (includes declarations from supers)

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        val allMethods = functions + getters + setters
        return allMethods
    }

    private fun exportTypeInfoIfRequired(classDesc: ClassDescriptor, typeInfoGlobal: LLVMValueRef?) {
        val annot = classDesc.annotations.findAnnotation(FqName("konan.ExportTypeInfo"))
        if (annot != null) {
            val nameValue = annot.allValueArguments.values.single() as StringValue
            // TODO: use LLVMAddAlias?
            val global = LLVMAddGlobal(context.llvmModule, pointerType(runtime.typeInfoType), nameValue.value)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            "kotlin.Array"        to -pointerSize,
            "kotlin.ByteArray"    to -1,
            "kotlin.CharArray"    to -2,
            "kotlin.ShortArray"   to -2,
            "kotlin.IntArray"     to -4,
            "kotlin.LongArray"    to -8,
            "kotlin.FloatArray"   to -4,
            "kotlin.DoubleArray"  to -8,
            "kotlin.BooleanArray" to -1,
            "kotlin.String"       to -1
    )

    private fun getInstanceSize(classType: LLVMTypeRef?, className: FqName) : Int {
        val arraySize = arrayClasses.get(className.asString());
        if (arraySize != null) return arraySize;
        return LLVMStoreSizeOfType(runtime.targetData, classType).toInt()
    }

    fun generate(classDesc: ClassDescriptor) {

        val className = classDesc.fqNameSafe

        val classType = createStructFor(className, classDesc.fields)

        val name = className.globalHash

        val size = getInstanceSize(classType, className)

        val superTypeOrAny = classDesc.getSuperClassOrAny()
        val superType = if (KotlinBuiltIns.isAny(classDesc)) NullPointer(runtime.typeInfoType)
                else superTypeOrAny.typeInfoPtr

        val interfaces = classDesc.implementedInterfaces.map { it.typeInfoPtr }
        val interfacesPtr = staticData.placeGlobalConstArray("kintf:$className",
                pointerType(runtime.typeInfoType), interfaces)

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
        val objOffsetsPtr = staticData.placeGlobalConstArray("krefs:$className", int32Type,
                objOffsets.map { Int32(it.toInt()) })

        val fields = classDesc.fields.mapIndexed { index, field ->
            // Note: using FQ name because a class may have multiple fields with the same name due to property overriding
            val nameSignature = field.fqNameSafe.localHash // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(runtime.targetData, classType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature.value }

        val fieldsPtr = staticData.placeGlobalConstArray("kfields:$className",
                runtime.fieldTableRecordType, fields)

        val vtable: List<ConstValue>
        val methods: List<ConstValue>

        if (classDesc.modality != Modality.ABSTRACT) {
            // TODO: compile-time resolution limits binary compatibility
            vtable = getVtableEntries(classDesc).map { it.implementation.entryPointAddress }

            methods = getMethodTableEntries(classDesc).map {
                val nameSignature = it.functionName.localHash
                // TODO: compile-time resolution limits binary compatibility
                val methodEntryPoint = it.implementation.entryPointAddress
                MethodTableRecord(nameSignature, methodEntryPoint)
            }.sortedBy { it.nameSignature.value }
        } else {
            vtable = emptyList()
            methods = emptyList()
        }


        val vtablePtr = staticData.placeGlobalConstArray("kvtable:$className", pointerType(int8Type), vtable)

        val methodsPtr = staticData.placeGlobalConstArray("kmethods:$className",
                runtime.methodTableRecordType, methods)

        val typeInfo = TypeInfo(name, size,
                superType,
                objOffsetsPtr, objOffsets.size,
                interfacesPtr, interfaces.size,
                vtablePtr,
                methodsPtr, methods.size,
                fieldsPtr, if (classDesc.isInterface) -1 else fields.size)

        val typeInfoGlobal = classDesc.llvmTypeInfoPtr // TODO: it is a hack
        LLVMSetInitializer(typeInfoGlobal, typeInfo.llvm)
        LLVMSetGlobalConstant(typeInfoGlobal, 1)

        exportTypeInfoIfRequired(classDesc, typeInfoGlobal)
    }

}
