/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.computePrimitiveBinaryTypeOrNull
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.isExternalObjCClassMethod
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameSafe
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.konan.KonanAbiVersion
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
        context.llvmDeclarations.forClass(irClass).fields.all { checkAcyclicFieldType(it.type) } -> true
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
        if (irClass.isInterface)
            result = result or TF_INTERFACE
        return result
    }

    inner class MethodTableRecord(val nameSignature: LocalHash, methodEntryPoint: ConstPointer?) :
            Struct(runtime.methodTableRecordType, nameSignature, methodEntryPoint)

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
            packageName: String?,
            relativeName: String?,
            flags: Int,
            writableTypeInfo: ConstPointer?) :

            Struct(
                    runtime.typeInfoType,

                    selfPtr,

                    extendedInfo,

                    Int32(KonanAbiVersion.CURRENT.version),

                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    methods,
                    Int32(methodsCount),

                    kotlinStringLiteral(packageName),
                    kotlinStringLiteral(relativeName),

                    Int32(flags),

                    *listOfNotNull(writableTypeInfo).toTypedArray()
            )

    private fun kotlinStringLiteral(string: String?): ConstPointer = if (string == null) {
        NullPointer(runtime.objHeaderType)
    } else {
        staticData.kotlinStringLiteral(string)
    }

    private val EXPORT_TYPE_INFO_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "ExportTypeInfo"))

    private fun exportTypeInfoIfRequired(irClass: IrClass, typeInfoGlobal: LLVMValueRef?) {
        val annotation = irClass.descriptor.annotations.findAnnotation(EXPORT_TYPE_INFO_FQ_NAME)
        if (annotation != null) {
            val name = getAnnotationValue(annotation)!!
            // TODO: use LLVMAddAlias.
            val global = addGlobal(name, pointerType(runtime.typeInfoType), isExported = true)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            "kotlin.Array"              to kObjHeaderPtr,
            "kotlin.ByteArray"          to LLVMInt8Type()!!,
            "kotlin.CharArray"          to LLVMInt16Type()!!,
            "kotlin.ShortArray"         to LLVMInt16Type()!!,
            "kotlin.IntArray"           to LLVMInt32Type()!!,
            "kotlin.LongArray"          to LLVMInt64Type()!!,
            "kotlin.FloatArray"         to LLVMFloatType()!!,
            "kotlin.DoubleArray"        to LLVMDoubleType()!!,
            "kotlin.BooleanArray"       to LLVMInt8Type()!!,
            "kotlin.String"             to LLVMInt16Type()!!,
            "kotlin.native.ImmutableBlob" to LLVMInt8Type()!!,
            "kotlin.native.internal.NativePtrArray" to kInt8Ptr
    )

    // Keep in sync with Konan_RuntimeType.
    private val runtimeTypeMap = mapOf(
            kObjHeaderPtr to 1,
            LLVMInt8Type()!! to 2,
            LLVMInt16Type()!! to 3,
            LLVMInt32Type()!! to 4,
            LLVMInt64Type()!! to 5,
            LLVMFloatType()!! to 6,
            LLVMDoubleType()!! to 7,
            kInt8Ptr to 8,
            LLVMInt1Type()!! to 9
    )

    private fun getInstanceSize(classType: LLVMTypeRef?, className: FqName) : Int {
        val elementType = arrayClasses.get(className.asString())
        // Check if it is an array.
        if (elementType != null) return -LLVMABISizeOfType(llvmTargetData, elementType).toInt()
        return LLVMStoreSizeOfType(llvmTargetData, classType).toInt()
    }

    fun generate(irClass: IrClass) {

        val className = irClass.fqNameSafe

        val llvmDeclarations = context.llvmDeclarations.forClass(irClass)

        val bodyType = llvmDeclarations.bodyType

        val size = getInstanceSize(bodyType, className)

        val superTypeOrAny = irClass.getSuperClassNotAny() ?: context.ir.symbols.any.owner
        val superType = if (irClass.isAny()) NullPointer(runtime.typeInfoType)
                else superTypeOrAny.typeInfoPtr

        val interfaces = irClass.implementedInterfaces.map { it.typeInfoPtr }
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

        val reflectionInfo = getReflectionInfo(irClass)
        val typeInfoGlobal = llvmDeclarations.typeInfoGlobal
        val typeInfo = TypeInfo(
                irClass.typeInfoPtr,
                makeExtendedInfo(irClass),
                size,
                superType,
                objOffsetsPtr, objOffsetsCount,
                interfacesPtr, interfaces.size,
                methodsPtr, methods.size,
                reflectionInfo.packageName,
                reflectionInfo.relativeName,
                flagsFromClass(irClass),
                llvmDeclarations.writableTypeInfoGlobal?.pointer
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
        val vtableEntries = context.getVtableBuilder(irClass).vtableEntries.map {
            val implementation = it.implementation
            if (implementation == null || implementation.isExternalObjCClassMethod()) {
                NullPointer(int8Type)
            } else {
                implementation.entryPointAddress
            }
        }
        return ConstArray(int8TypePtr, vtableEntries)
    }

    fun methodTableRecords(irClass: IrClass): List<MethodTableRecord> {
        val functionNames = mutableMapOf<Long, OverriddenFunctionInfo>()
        return context.getVtableBuilder(irClass).methodTableEntries.map {
            val functionName = it.overriddenFunction.functionName
            val nameSignature = functionName.localHash
            val previous = functionNames.putIfAbsent(nameSignature.value, it)
            if (previous != null)
                throw AssertionError("Duplicate method table entry: functionName = '$functionName', hash = '${nameSignature.value}', entry1 = $previous, entry2 = $it")

            // TODO: compile-time resolution limits binary compatibility.
            val implementation = it.implementation
            val methodEntryPoint = implementation?.entryPointAddress
            MethodTableRecord(nameSignature, methodEntryPoint)
        }.sortedBy { it.nameSignature.value }
    }

    private fun mapRuntimeType(type: LLVMTypeRef): Int =
            runtimeTypeMap[type] ?: throw Error("Unmapped type: ${llvmtype2string(type)}")

    private fun makeExtendedInfo(irClass: IrClass): ConstPointer {
        // TODO: shall we actually do that?
        if (context.shouldOptimize())
            return NullPointer(runtime.extendedTypeInfoType)

        val className = irClass.fqNameSafe.toString()
        val llvmDeclarations = context.llvmDeclarations.forClass(irClass)
        val bodyType = llvmDeclarations.bodyType
        val elementType = arrayClasses[className]
        val value = if (elementType != null) {
            // An array type.
            val runtimeElementType = mapRuntimeType(elementType)
            Struct(runtime.extendedTypeInfoType,
                    Int32(-runtimeElementType),
                    NullPointer(int32Type), NullPointer(int8Type), NullPointer(kInt8Ptr))
        } else {
            data class FieldRecord(val offset: Int, val type: Int, val name: String)
            val fields = getStructElements(bodyType).drop(1).mapIndexedNotNull { index, type ->
                FieldRecord(
                        LLVMOffsetOfElement(llvmTargetData, bodyType, index + 1).toInt(),
                        mapRuntimeType(type),
                        llvmDeclarations.fields[index].name.asString())
            }
            val offsetsPtr = staticData.placeGlobalConstArray("kextoff:$className", int32Type,
                    fields.map { Int32(it.offset) })
            val typesPtr = staticData.placeGlobalConstArray("kexttype:$className", int8Type,
                    fields.map { Int8(it.type.toByte()) })
            val namesPtr = staticData.placeGlobalConstArray("kextname:$className", kInt8Ptr,
                    fields.map { staticData.placeCStringLiteral(it.name) })
            Struct(runtime.extendedTypeInfoType, Int32(fields.size), offsetsPtr, typesPtr, namesPtr)
        }
        val result = staticData.placeGlobal("", value)
        result.setConstant(true)
        return result.pointer
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
        val interfaces = listOf(irClass.typeInfoPtr)
        val interfacesPtr = staticData.placeGlobalConstArray("",
                pointerType(runtime.typeInfoType), interfaces)

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
        val typeInfoWithVtable = Struct(TypeInfo(
                selfPtr = result,
                extendedInfo = NullPointer(runtime.extendedTypeInfoType),
                size = size,
                superType = superClass.typeInfoPtr,
                objOffsets = objOffsetsPtr, objOffsetsCount = objOffsetsCount,
                interfaces = interfacesPtr, interfacesCount = interfaces.size,
                methods = methodsPtr, methodsCount = methods.size,
                packageName = reflectionInfo.packageName,
                relativeName = reflectionInfo.relativeName,
                flags = flagsFromClass(irClass) or (if (immutable) TF_IMMUTABLE else 0),
                writableTypeInfo = writableTypeInfo
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
                packageName = irClass.findPackage().fqName.asString(),
                relativeName = generateSequence(irClass) { it.parent as? IrClass }
                        .toList().reversed()
                        .joinToString(".") { it.name.asString() }
        )
    }
}

// Keep in sync with Konan_TypeFlags in TypeInfo.h.
private const val TF_IMMUTABLE = 1
private const val TF_ACYCLIC   = 2
private const val TF_INTERFACE = 4
