/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.isExternalObjCClassMethod
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.name.FqName

internal class RTTIGenerator(override val context: Context) : ContextUtils {

    private val acyclicCache = mutableMapOf<IrType, Boolean>()
    private val safeAcyclicFieldTypes = setOf(
            context.irBuiltIns.stringClass,
            context.irBuiltIns.booleanClass, context.irBuiltIns.charClass,
            context.irBuiltIns.byteClass, context.irBuiltIns.shortClass, context.irBuiltIns.intClass,
            context.irBuiltIns.longClass,
            context.irBuiltIns.floatClass,context.irBuiltIns.doubleClass) +
            context.ir.symbols.primitiveArrays.values +
            context.ir.symbols.unsignedArrays.values

    // TODO: extend logic here by taking into account final acyclic classes.
    private fun checkAcyclicFieldType(type: IrType): Boolean = acyclicCache.getOrPut(type) {
        when {
            type.isInterface() -> false
            type.computePrimitiveBinaryTypeOrNull() != null -> true
            else -> {
                val classifier = type.classifierOrNull
                (classifier != null && classifier in safeAcyclicFieldTypes)
            }
        }
    }

    private fun checkAcyclicClass(irClass: IrClass): Boolean = when {
        irClass.symbol == context.ir.symbols.array -> false
        irClass.isArray -> true
        context.getLayoutBuilder(irClass).fields.all { checkAcyclicFieldType(it.type) } -> true
        else -> false
    }

    private fun flagsFromClass(irClass: IrClass): Int {
        var result = 0
        if (irClass.isFrozen)
           result = result or TF_IMMUTABLE
        // TODO: maybe perform deeper analysis to find surely acyclic types.
        if (!irClass.isInterface && !irClass.isAbstract() && !irClass.isAnnotationClass) {
            if (checkAcyclicClass(irClass)) {
                result = result or TF_ACYCLIC
            }
        }
        if (irClass.hasAnnotation(KonanFqNames.leakDetectorCandidate)) {
            result = result or TF_LEAK_DETECTOR_CANDIDATE
        }
        if (irClass.isInterface)
            result = result or TF_INTERFACE

        if (irClass.defaultType.isSuspendFunction()) {
            result = result or TF_SUSPEND_FUNCTION
        }

        if (irClass.hasAnnotation(KonanFqNames.hasFinalizer)) {
            result = result or TF_HAS_FINALIZER
        }

        return result
    }

    inner class MethodTableRecord(val nameSignature: LocalHash, methodEntryPoint: ConstPointer?) :
            Struct(runtime.methodTableRecordType, nameSignature, methodEntryPoint)

    inner class InterfaceTableRecord(id: Int32, vtableSize: Int32, vtable: ConstPointer?) :
            Struct(runtime.interfaceTableRecordType, id, vtableSize, vtable)

    private inner class TypeInfo(
            selfPtr: ConstPointer,
            extendedInfo: ConstPointer,
            size: Int,
            superType: ConstValue,
            objOffsets: ConstValue,
            objOffsetsCount: Int,
            interfaces: ConstValue,
            interfacesCount: Int,
            methods: ConstValue,
            methodsCount: Int,
            interfaceTableSize: Int,
            interfaceTable: ConstValue,
            packageName: String?,
            relativeName: String?,
            flags: Int,
            classId: Int,
            writableTypeInfo: ConstPointer?,
            associatedObjects: ConstPointer?) :

            Struct(
                    runtime.typeInfoType,

                    selfPtr,

                    extendedInfo,

                    // TODO: it used to be a single int32 ABI version,
                    // but klib abi version is not an int anymore.
                    // So now this field is just reserved to preserve the layout.
                    Int32(0),

                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    methods,
                    Int32(methodsCount),

                    Int32(interfaceTableSize),
                    interfaceTable,

                    kotlinStringLiteral(packageName),
                    kotlinStringLiteral(relativeName),

                    Int32(flags),

                    Int32(classId),

                    *listOfNotNull(writableTypeInfo).toTypedArray(),

                    associatedObjects
            )

    private fun kotlinStringLiteral(string: String?): ConstPointer = if (string == null) {
        NullPointer(runtime.objHeaderType)
    } else {
        staticData.kotlinStringLiteral(string)
    }

    private val EXPORT_TYPE_INFO_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "ExportTypeInfo"))

    private fun exportTypeInfoIfRequired(irClass: IrClass, typeInfoGlobal: LLVMValueRef?) {
        val annotation = irClass.annotations.findAnnotation(EXPORT_TYPE_INFO_FQ_NAME)
        if (annotation != null) {
            val name = annotation.getAnnotationStringValue()!!
            // TODO: use LLVMAddAlias.
            val global = addGlobal(name, pointerType(runtime.typeInfoType), isExported = true)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            "kotlin.Array"              to kObjHeaderPtr,
            "kotlin.ByteArray"          to int8Type,
            "kotlin.CharArray"          to int16Type,
            "kotlin.ShortArray"         to int16Type,
            "kotlin.IntArray"           to int32Type,
            "kotlin.LongArray"          to int64Type,
            "kotlin.FloatArray"         to floatType,
            "kotlin.DoubleArray"        to doubleType,
            "kotlin.BooleanArray"       to int8Type,
            "kotlin.String"             to int16Type,
            "kotlin.native.ImmutableBlob" to int8Type,
            "kotlin.native.internal.NativePtrArray" to kInt8Ptr
    )

    // Keep in sync with Konan_RuntimeType.
    private val runtimeTypeMap = mapOf(
            kObjHeaderPtr to 1,
            int8Type to 2,
            int16Type to 3,
            int32Type to 4,
            int64Type to 5,
            floatType to 6,
            doubleType to 7,
            kInt8Ptr to 8,
            int1Type to 9,
            vector128Type to 10
    )

    private fun getInstanceSize(classType: LLVMTypeRef?, className: FqName) : Int {
        val elementType = arrayClasses.get(className.asString())
        // Check if it is an array.
        if (elementType != null) return -LLVMABISizeOfType(llvmTargetData, elementType).toInt()
        return LLVMStoreSizeOfType(llvmTargetData, classType).toInt()
    }

    private fun getClassId(irClass: IrClass): Int {
        if (irClass.isKotlinObjCClass()) return 0
        val hierarchyInfo = if (context.ghaEnabled()) {
            context.getLayoutBuilder(irClass).hierarchyInfo
        } else {
            ClassGlobalHierarchyInfo.DUMMY
        }
        return if (irClass.isInterface) {
            hierarchyInfo.interfaceId
        } else {
            hierarchyInfo.classIdLo
        }
    }

    fun generate(irClass: IrClass) {

        val className = irClass.fqNameForIrSerialization

        val llvmDeclarations = context.llvmDeclarations.forClass(irClass)

        val bodyType = llvmDeclarations.bodyType

        val instanceSize = getInstanceSize(bodyType, className)

        val superType = when {
            irClass.isAny() -> NullPointer(runtime.typeInfoType)
            irClass.isKotlinObjCClass() -> context.ir.symbols.any.owner.typeInfoPtr
            else -> {
                val superTypeOrAny = irClass.getSuperClassNotAny() ?: context.ir.symbols.any.owner
                superTypeOrAny.typeInfoPtr
            }
        }

        val implementedInterfaces = irClass.implementedInterfaces.filter { it.requiresRtti() }

        val interfaces = implementedInterfaces.map { it.typeInfoPtr }
        val interfacesPtr = staticData.placeGlobalConstArray("kintf:$className",
                pointerType(runtime.typeInfoType), interfaces)

        val objOffsets = getObjOffsets(bodyType)

        val objOffsetsPtr = staticData.placeGlobalConstArray("krefs:$className", int32Type, objOffsets)

        val objOffsetsCount = if (irClass.descriptor == context.builtIns.array) {
            1 // To mark it as non-leaf.
        } else {
            objOffsets.size
        }

        val methods = if (irClass.isAbstract()) {
            emptyList()
        } else {
            methodTableRecords(irClass)
        }
        val methodsPtr = staticData.placeGlobalConstArray("kmethods:$className",
                runtime.methodTableRecordType, methods)

        val needInterfaceTable = context.ghaEnabled() && !irClass.isInterface
                && !irClass.isAbstract() && !irClass.isObjCClass()
        val (interfaceTable, interfaceTableSize) = if (needInterfaceTable) {
            interfaceTableRecords(irClass)
        } else {
            Pair(emptyList(), -1)
        }
        val interfaceTablePtr = staticData.placeGlobalConstArray("kifacetable:$className",
                runtime.interfaceTableRecordType, interfaceTable)

        val reflectionInfo = getReflectionInfo(irClass)
        val typeInfoGlobal = llvmDeclarations.typeInfoGlobal
        val typeInfo = TypeInfo(
                irClass.typeInfoPtr,
                makeExtendedInfo(irClass),
                instanceSize,
                superType,
                objOffsetsPtr, objOffsetsCount,
                interfacesPtr, interfaces.size,
                methodsPtr, methods.size,
                interfaceTableSize, interfaceTablePtr,
                reflectionInfo.packageName,
                reflectionInfo.relativeName,
                flagsFromClass(irClass),
                getClassId(irClass),
                llvmDeclarations.writableTypeInfoGlobal?.pointer,
                associatedObjects = genAssociatedObjects(irClass)
        )

        val typeInfoGlobalValue = if (!irClass.typeInfoHasVtableAttached) {
            typeInfo
        } else {
            val vtable = vtable(irClass)
            Struct(typeInfo, vtable)
        }

        typeInfoGlobal.setInitializer(typeInfoGlobalValue)
        typeInfoGlobal.setConstant(true)

        exportTypeInfoIfRequired(irClass, irClass.llvmTypeInfoPtr)
    }

    private fun getObjOffsets(bodyType: LLVMTypeRef): List<Int32> =
            getStructElements(bodyType).mapIndexedNotNull { index, type ->
                if (isObjectType(type)) {
                    LLVMOffsetOfElement(llvmTargetData, bodyType, index)
                } else {
                    null
                }
            }.map { Int32(it.toInt()) }

    fun vtable(irClass: IrClass): ConstArray {
        // TODO: compile-time resolution limits binary compatibility.
        val vtableEntries = context.getLayoutBuilder(irClass).vtableEntries.map {
            val implementation = it.implementation
            if (implementation == null || implementation.isExternalObjCClassMethod() || context.referencedFunctions?.contains(implementation) == false) {
                NullPointer(int8Type)
            } else {
                implementation.entryPointAddress
            }
        }
        return ConstArray(int8TypePtr, vtableEntries)
    }

    fun methodTableRecords(irClass: IrClass): List<MethodTableRecord> {
        val functionNames = mutableMapOf<Long, OverriddenFunctionInfo>()
        return context.getLayoutBuilder(irClass).methodTableEntries.map {
            val functionName = it.overriddenFunction.functionName
            val nameSignature = functionName.localHash
            val previous = functionNames.putIfAbsent(nameSignature.value, it)
            if (previous != null)
                throw AssertionError("Duplicate method table entry: functionName = '$functionName', hash = '${nameSignature.value}', entry1 = $previous, entry2 = $it")

            // TODO: compile-time resolution limits binary compatibility.
            val implementation = it.implementation
            val methodEntryPoint =
                if (implementation == null || context.referencedFunctions?.contains(implementation) == false)
                    null
                else implementation.entryPointAddress
            MethodTableRecord(nameSignature, methodEntryPoint)
        }.sortedBy { it.nameSignature.value }
    }

    fun interfaceTableRecords(irClass: IrClass): Pair<List<InterfaceTableRecord>, Int> {
        // The details are in ClassLayoutBuilder.
        val interfaces = irClass.implementedInterfaces
        val (interfaceTableSkeleton, interfaceTableSize) = interfaceTableSkeleton(interfaces)

        val interfaceTableEntries = interfaceTableRecords(irClass, interfaceTableSkeleton)
        return Pair(interfaceTableEntries, interfaceTableSize)
    }

    private fun interfaceTableSkeleton(interfaces: List<IrClass>): Pair<Array<out ClassLayoutBuilder?>, Int> {
        val interfaceLayouts = interfaces.map { context.getLayoutBuilder(it) }
        val interfaceColors = interfaceLayouts.map { it.hierarchyInfo.interfaceColor }

        // Find the optimal size. It must be a power of 2.
        var size = 1
        val maxSize = 1 shl ClassGlobalHierarchyInfo.MAX_BITS_PER_COLOR
        val used = BooleanArray(maxSize)
        while (size <= maxSize) {
            for (i in 0 until size)
                used[i] = false
            // Check for collisions.
            var ok = true
            for (color in interfaceColors) {
                val index = color % size
                if (used[index]) {
                    ok = false
                    break
                }
                used[index] = true
            }
            if (ok) break
            size *= 2
        }
        val conservative = size > maxSize

        val interfaceTableSkeleton = if (conservative) {
            size = interfaceLayouts.size
            interfaceLayouts.sortedBy { it.hierarchyInfo.interfaceId }.toTypedArray()
        } else arrayOfNulls<ClassLayoutBuilder?>(size).also {
            for (interfaceLayout in interfaceLayouts)
                it[interfaceLayout.hierarchyInfo.interfaceId % size] = interfaceLayout
        }

        val interfaceTableSize = if (conservative) -size else (size - 1)
        return Pair(interfaceTableSkeleton, interfaceTableSize)
    }

    private fun interfaceTableRecords(
            irClass: IrClass,
            interfaceTableSkeleton: Array<out ClassLayoutBuilder?>
    ): List<InterfaceTableRecord> {
        val methodTableEntries = context.getLayoutBuilder(irClass).methodTableEntries
        val className = irClass.fqNameForIrSerialization

        return interfaceTableSkeleton.map { iface ->
            val interfaceId = iface?.hierarchyInfo?.interfaceId ?: 0
            InterfaceTableRecord(
                    Int32(interfaceId),
                    Int32(iface?.interfaceTableEntries?.size ?: 0),
                    if (iface == null)
                        NullPointer(kInt8Ptr)
                    else {
                        val vtableEntries = iface.interfaceTableEntries.map { ifaceFunction ->
                            val impl = OverriddenFunctionInfo(
                                    methodTableEntries.first { ifaceFunction in it.function.allOverriddenFunctions }.function,
                                    ifaceFunction
                            ).implementation
                            if (impl == null || context.referencedFunctions?.contains(impl) == false)
                                NullPointer(int8Type)
                            else impl.entryPointAddress
                        }

                        staticData.placeGlobalConstArray("kifacevtable:${className}_$interfaceId",
                                kInt8Ptr, vtableEntries
                        )
                    }
            )
        }
    }

    private fun mapRuntimeType(type: LLVMTypeRef): Int =
            runtimeTypeMap[type] ?: throw Error("Unmapped type: ${llvmtype2string(type)}")

    private val debugRuntimeOrNull: LLVMModuleRef? by lazy {
        context.config.runtimeNativeLibraries.singleOrNull { it.endsWith("debug.bc")}?.let {
            parseBitcodeFile(it)
        }
    }

    private val debugOperations: ConstValue by lazy {
        if (debugRuntimeOrNull != null) {
            val external = LLVMGetNamedGlobal(debugRuntimeOrNull, "Konan_debugOperationsList")!!
            val local = LLVMAddGlobal(context.llvmModule, LLVMGetElementType(LLVMTypeOf(external)),"Konan_debugOperationsList")!!
            constPointer(LLVMConstBitCast(local, kInt8PtrPtr)!!)
        } else {
            Zero(kInt8PtrPtr)
        }
    }

    val debugOperationsSize: ConstValue by lazy {
        if (debugRuntimeOrNull != null) {
            val external = LLVMGetNamedGlobal(debugRuntimeOrNull, "Konan_debugOperationsList")!!
            Int32(LLVMGetArrayLength(LLVMGetElementType(LLVMTypeOf(external))))
        } else
            Int32(0)
    }

    private fun makeExtendedInfo(irClass: IrClass): ConstPointer {
        // TODO: shall we actually do that?
        if (context.shouldOptimize())
            return NullPointer(runtime.extendedTypeInfoType)

        val className = irClass.fqNameForIrSerialization.toString()
        val llvmDeclarations = context.llvmDeclarations.forClass(irClass)
        val bodyType = llvmDeclarations.bodyType
        val elementType = arrayClasses[className]

        val value = if (elementType != null) {
            // An array type.
            val runtimeElementType = mapRuntimeType(elementType)
            Struct(runtime.extendedTypeInfoType,
                    Int32(-runtimeElementType),
                    NullPointer(int32Type), NullPointer(int8Type), NullPointer(kInt8Ptr),
                    debugOperationsSize, debugOperations)
        } else {
            data class FieldRecord(val offset: Int, val type: Int, val name: String)
            val fields = getStructElements(bodyType).drop(1).mapIndexed { index, type ->
                FieldRecord(
                        LLVMOffsetOfElement(llvmTargetData, bodyType, index + 1).toInt(),
                        mapRuntimeType(type),
                        context.getLayoutBuilder(irClass).fields[index].name.asString())
            }
            val offsetsPtr = staticData.placeGlobalConstArray("kextoff:$className", int32Type,
                    fields.map { Int32(it.offset) })
            val typesPtr = staticData.placeGlobalConstArray("kexttype:$className", int8Type,
                    fields.map { Int8(it.type.toByte()) })
            val namesPtr = staticData.placeGlobalConstArray("kextname:$className", kInt8Ptr,
                    fields.map { staticData.placeCStringLiteral(it.name) })

            Struct(runtime.extendedTypeInfoType, Int32(fields.size), offsetsPtr, typesPtr, namesPtr,
                    debugOperationsSize, debugOperations)

        }
        val result = staticData.placeGlobal("", value)
        result.setConstant(true)
        return result.pointer
    }

    private fun genAssociatedObjects(irClass: IrClass): ConstPointer? {
        val associatedObjects = context.getLayoutBuilder(irClass).associatedObjects
        if (associatedObjects.isEmpty()) {
            return null
        }

        val associatedObjectTableRecords = associatedObjects.map { (key, value) ->
            val associatedObjectGetter = generateFunction(
                    CodeGenerator(context),
                    functionType(kObjHeaderPtr, false, kObjHeaderPtrPtr),
                    ""
            ) {
                ret(getObjectValue(value, ExceptionHandler.Caller, startLocationInfo = null))
            }

            Struct(runtime.associatedObjectTableRecordType, key.typeInfoPtr, constPointer(associatedObjectGetter))
        }

        return staticData.placeGlobalConstArray(
                name = "kassociatedobjects:${irClass.fqNameForIrSerialization}",
                elemType = runtime.associatedObjectTableRecordType,
                elements = associatedObjectTableRecords + Struct(runtime.associatedObjectTableRecordType, null, null)
        )
    }

    // TODO: extract more code common with generate().
    fun generateSyntheticInterfaceImpl(
            irClass: IrClass,
            methodImpls: Map<IrFunction, ConstPointer>,
            bodyType: LLVMTypeRef,
            immutable: Boolean = false
    ): ConstPointer {
        assert(irClass.isInterface)

        val size = LLVMStoreSizeOfType(llvmTargetData, bodyType).toInt()

        val superClass = context.ir.symbols.any.owner

        assert(superClass.implementedInterfaces.isEmpty())
        val interfaces = (listOf(irClass) + irClass.implementedInterfaces)
        val interfacesPtr = staticData.placeGlobalConstArray("",
                pointerType(runtime.typeInfoType), interfaces.map { it.typeInfoPtr })

        assert(superClass.declarations.all { it !is IrProperty && it !is IrField })

        val objOffsets = getObjOffsets(bodyType)
        val objOffsetsPtr = staticData.placeGlobalConstArray("", int32Type, objOffsets)
        val objOffsetsCount = objOffsets.size

        val methods = (methodTableRecords(superClass) + methodImpls.map { (method, impl) ->
            assert(method.parent == irClass)
            MethodTableRecord(method.functionName.localHash, impl.bitcast(int8TypePtr))
        }).sortedBy { it.nameSignature.value }.also {
            assert(it.distinctBy { it.nameSignature.value } == it)
        }

        val methodsPtr = staticData.placeGlobalConstArray("", runtime.methodTableRecordType, methods)

        val reflectionInfo = ReflectionInfo(null, null)

        val writableTypeInfoType = runtime.writableTypeInfoType
        val writableTypeInfo = if (writableTypeInfoType == null) {
            null
        } else {
            staticData.createGlobal(writableTypeInfoType, "")
                    .also { it.setZeroInitializer() }
                    .pointer
        }
        val vtable = vtable(superClass)
        val typeInfoWithVtableType = structType(runtime.typeInfoType, vtable.llvmType)
        val typeInfoWithVtableGlobal = staticData.createGlobal(typeInfoWithVtableType, "", isExported = false)
        val result = typeInfoWithVtableGlobal.pointer.getElementPtr(0)
        val typeHierarchyInfo = if (!context.ghaEnabled())
            ClassGlobalHierarchyInfo.DUMMY
        else
            ClassGlobalHierarchyInfo(-1, -1, 0, 0)

        // TODO: interfaces (e.g. FunctionN and Function) should have different colors.
        val (interfaceTableSkeleton, interfaceTableSize) =
                if (context.ghaEnabled()) interfaceTableSkeleton(interfaces) else Pair(emptyArray(), -1)

        val interfaceTable = interfaceTableSkeleton.map { layoutBuilder ->
            if (layoutBuilder == null) {
                InterfaceTableRecord(Int32(0), Int32(0), null)
            } else {
                val vtableEntries = layoutBuilder.interfaceTableEntries.map { methodImpls[it]!!.bitcast(int8TypePtr) }
                val interfaceVTable = staticData.placeGlobalArray("", kInt8Ptr, vtableEntries)
                InterfaceTableRecord(
                        Int32(layoutBuilder.hierarchyInfo.interfaceId),
                        Int32(layoutBuilder.interfaceTableEntries.size),
                        interfaceVTable.pointer.getElementPtr(0)
                )
            }
        }
        val interfaceTablePtr = staticData.placeGlobalConstArray("", runtime.interfaceTableRecordType, interfaceTable)

        val typeInfoWithVtable = Struct(TypeInfo(
                selfPtr = result,
                extendedInfo = NullPointer(runtime.extendedTypeInfoType),
                size = size,
                superType = superClass.typeInfoPtr,
                objOffsets = objOffsetsPtr, objOffsetsCount = objOffsetsCount,
                interfaces = interfacesPtr, interfacesCount = interfaces.size,
                methods = methodsPtr, methodsCount = methods.size,
                interfaceTableSize = interfaceTableSize, interfaceTable = interfaceTablePtr,
                packageName = reflectionInfo.packageName,
                relativeName = reflectionInfo.relativeName,
                flags = flagsFromClass(irClass) or (if (immutable) TF_IMMUTABLE else 0),
                classId = typeHierarchyInfo.classIdLo,
                writableTypeInfo = writableTypeInfo,
                associatedObjects = null
              ), vtable)

        typeInfoWithVtableGlobal.setInitializer(typeInfoWithVtable)
        typeInfoWithVtableGlobal.setConstant(true)

        return result
    }

    private val OverriddenFunctionInfo.implementation get() = getImplementation(context)

    data class ReflectionInfo(val packageName: String?, val relativeName: String?)

    private fun getReflectionInfo(irClass: IrClass): ReflectionInfo = when {
        irClass.isAnonymousObject -> ReflectionInfo(packageName = null, relativeName = null)

        irClass.isLocal -> ReflectionInfo(packageName = null, relativeName = irClass.name.asString())

        else -> ReflectionInfo(
                packageName = irClass.findPackage().fqNameForIrSerialization.asString(),
                relativeName = generateSequence(irClass) { it.parent as? IrClass }
                        .toList().reversed()
                        .joinToString(".") { it.name.asString() }
        )
    }

    fun dispose() {
        debugRuntimeOrNull?.let { LLVMDisposeModule(it) }
    }
}

// Keep in sync with Konan_TypeFlags in TypeInfo.h.
private const val TF_IMMUTABLE = 1
private const val TF_ACYCLIC   = 2
private const val TF_INTERFACE = 4
private const val TF_OBJC_DYNAMIC = 8
private const val TF_LEAK_DETECTOR_CANDIDATE = 16
private const val TF_SUSPEND_FUNCTION = 32
private const val TF_HAS_FINALIZER = 64

