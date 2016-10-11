package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.mallocNativeArrayOf
import llvm.*
import org.jetbrains.kotlin.backend.native.implementation
import org.jetbrains.kotlin.backend.native.implementedInterfaces
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.OverridingUtil
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
				 val vtable: CompileTimeValue,
				 val methods: CompileTimeValue,
				 val methodsCount: Int,
				 val fields: CompileTimeValue,
				 val fieldsCount: Int) :
	    Struct(
		    runtime.typeInfoType,

		    Struct(runtime.globalhHashType, ConstArray(LLVMInt8Type(), Array(20, {i -> Int8(1)}).toList())),
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

    // TODO: optimize
    private fun getVtableEntries(classDesc: ClassDescriptor): List<FunctionDescriptor> {
	val superVtableEntries = if (KotlinBuiltIns.isAny(classDesc)) {
	    emptyList()
	} else {
	    getVtableEntries(classDesc.getSuperClassOrAny())
	}

	val inheritedVtableSlots = Array<FunctionDescriptor?>(superVtableEntries.size, { null })
	val methods = getMethodTableEntries(classDesc) // TODO: ensure order is well-defined
	val entriesForNewVtableSlots = methods.toMutableSet()

	methods.forEach { method ->
	    superVtableEntries.forEachIndexed { i, superMethod ->
		if (OverridingUtil.overrides(method, superMethod)) {
		    assert (inheritedVtableSlots[i] == null)
		    inheritedVtableSlots[i] = method

		    assert (method in entriesForNewVtableSlots)
		    entriesForNewVtableSlots.remove(method)
		}
	    }
	}

	return inheritedVtableSlots.map { it!! } + methods.filter { it in entriesForNewVtableSlots }
    }

    private fun getMethodTableEntries(classDesc: ClassDescriptor): List<FunctionDescriptor> {
	val contributedDescriptors = classDesc.unsubstitutedMemberScope.getContributedDescriptors()
	 // (includes declarations from supers)

	val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

	val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
	val getters = properties.mapNotNull { it.getter }
	val setters = properties.mapNotNull { it.setter }

	val allMethods = functions + getters + setters

	// TODO: adding or removing 'open' modifier will break the binary compatibility
	return allMethods.filter { it.modality != Modality.FINAL }
    }

    fun generate(classDesc: ClassDescriptor) {

	val className = classDesc.fqNameSafe

	val classType = createStructFor(className, classDesc.fields)

	val name = className.nameHash

	val size = LLVMStoreSizeOfType(runtime.targetData, classType).toInt()

	val superType = classDesc.getSuperClassOrAny().llvmTypeInfoPtr

	val interfaces = classDesc.implementedInterfaces.map { it.llvmTypeInfoPtr }
	val interfacesPtr = addGlobalConstArray("kintf:$className", pointerType(runtime.typeInfoType), interfaces)

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
	val objOffsetsPtr = addGlobalConstArray("krefs:$className", int32Type, objOffsets.map { Int32(it.toInt()) })

	val fields = classDesc.fields.mapIndexed { index, field ->
	    // Note: using FQ name because a class may have multiple fields with the same name due to property overriding
	    val nameSignature = field.fqNameSafe.nameHash // FIXME: add signature
	    val fieldOffset = LLVMOffsetOfElement(runtime.targetData, classType, index)
	    FieldTableRecord(nameSignature, fieldOffset.toInt())
	}.sortedBy { it.nameSignature }

	val fieldsPtr = addGlobalConstArray("kfields:$className", runtime.fieldTableRecordType, fields)

	// TODO: compile-time resolution limits binary compatibility
	val vtable = getVtableEntries(classDesc).map { it.implementation.entryPointAddress }
	val vtablePtr = addGlobalConstArray("kvtable:$className", pointerType(int8Type), vtable)

	val methods = getMethodTableEntries(classDesc).map {
	    val nameSignature = it.name.nameHash // FIXME: add signature
	    // TODO: compile-time resolution limits binary compatibility
	    val methodEntryPoint = it.implementation.entryPointAddress
	    MethodTableRecord(nameSignature, methodEntryPoint)
	}.sortedBy { it.nameSignature }

	val methodsPtr = addGlobalConstArray("kmethods:$className", runtime.methodTableRecordType, methods)

	val typeInfo = TypeInfo(name, size,
				superType,
				objOffsetsPtr, objOffsets.size,
				interfacesPtr, interfaces.size,
				vtablePtr,
				methodsPtr, methods.size,
				fieldsPtr, fields.size)

	val typeInfoGlobal = classDesc.llvmTypeInfoPtr.getLlvmValue() // TODO: it is a hack
	LLVMSetInitializer(typeInfoGlobal, typeInfo.getLlvmValue())
	LLVMSetGlobalConstant(typeInfoGlobal, 1)
    }

}
