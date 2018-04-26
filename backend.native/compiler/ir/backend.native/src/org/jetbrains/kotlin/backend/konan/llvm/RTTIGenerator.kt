/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue


internal class RTTIGenerator(override val context: Context) : ContextUtils {

    private fun flagsFromClass(classDescriptor: ClassDescriptor): Int {
        var result = 0
        if (classDescriptor.isFrozen)
           result = result or 1 /* TF_IMMUTABLE */
        return result
    }

    private inner class FieldTableRecord(val nameSignature: LocalHash, val fieldOffset: Int) :
            Struct(runtime.fieldTableRecordType, nameSignature, Int32(fieldOffset))

    inner class MethodTableRecord(val nameSignature: LocalHash, val methodEntryPoint: ConstPointer?) :
            Struct(runtime.methodTableRecordType, nameSignature, methodEntryPoint)

    private inner class TypeInfo(
            selfPtr: ConstPointer,
            name: ConstValue,
            size: Int,
            superType: ConstValue,
            objOffsets: ConstValue,
            objOffsetsCount: Int,
            interfaces: ConstValue,
            interfacesCount: Int,
            methods: ConstValue,
            methodsCount: Int,
            fields: ConstValue,
            fieldsCount: Int,
            packageName: String?,
            relativeName: String?,
            flags: Int,
            extendedInfo: ConstPointer,
            writableTypeInfo: ConstPointer?) :

            Struct(
                    runtime.typeInfoType,

                    selfPtr,

                    name,
                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    methods,
                    Int32(methodsCount),

                    fields,
                    Int32(fieldsCount),

                    kotlinStringLiteral(packageName),
                    kotlinStringLiteral(relativeName),

                    Int32(flags),

                    extendedInfo,

                    *listOfNotNull(writableTypeInfo).toTypedArray()
            )

    private fun kotlinStringLiteral(string: String?): ConstPointer = if (string == null) {
        NullPointer(runtime.objHeaderType)
    } else {
        staticData.kotlinStringLiteral(string)
    }

    private fun exportTypeInfoIfRequired(classDesc: ClassDescriptor, typeInfoGlobal: LLVMValueRef?) {
        val annot = classDesc.descriptor.annotations.findAnnotation(FqName("konan.ExportTypeInfo"))
        if (annot != null) {
            val name = getStringValue(annot)!!
            // TODO: use LLVMAddAlias?
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
            "konan.ImmutableBinaryBlob" to LLVMInt8Type()!!
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

    fun generate(classDesc: ClassDescriptor) {

        val className = classDesc.fqNameSafe

        val llvmDeclarations = context.llvmDeclarations.forClass(classDesc)

        val bodyType = llvmDeclarations.bodyType

        val name = className.globalHash

        val size = getInstanceSize(bodyType, className)

        val superTypeOrAny = classDesc.getSuperClassNotAny() ?: context.ir.symbols.any.owner
        val superType = if (classDesc.isAny()) NullPointer(runtime.typeInfoType)
                else superTypeOrAny.typeInfoPtr

        val interfaces = classDesc.implementedInterfaces.map { it.typeInfoPtr }
        val interfacesPtr = staticData.placeGlobalConstArray("kintf:$className",
                pointerType(runtime.typeInfoType), interfaces)

        // TODO: reuse offsets obtained for 'fields' below
        val objOffsets = getStructElements(bodyType).mapIndexedNotNull { index, type ->
            if (isObjectType(type)) {
                LLVMOffsetOfElement(llvmTargetData, bodyType, index)
            } else {
                null
            }
        }

        val objOffsetsPtr = staticData.placeGlobalConstArray("krefs:$className", int32Type,
                objOffsets.map { Int32(it.toInt()) })

        val objOffsetsCount = if (classDesc.descriptor == context.builtIns.array) {
            1 // To mark it as non-leaf.
        } else {
            objOffsets.size
        }

        val fields = llvmDeclarations.fields.mapIndexed { index, field ->
            // Note: using FQ name because a class may have multiple fields with the same name due to property overriding
            val nameSignature = field.fqNameSafe.localHash // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(llvmTargetData, bodyType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature.value }

        val fieldsPtr = staticData.placeGlobalConstArray("kfields:$className",
                runtime.fieldTableRecordType, fields)

        val methods = if (classDesc.isAbstract()) {
            emptyList()
        } else {
            methodTableRecords(classDesc)
        }

        val methodsPtr = staticData.placeGlobalConstArray("kmethods:$className",
                runtime.methodTableRecordType, methods)

        val reflectionInfo = getReflectionInfo(classDesc)
        val typeInfoGlobal = llvmDeclarations.typeInfoGlobal
        val typeInfo = TypeInfo(
                classDesc.typeInfoPtr,
                name,
                size,
                superType,
                objOffsetsPtr, objOffsetsCount,
                interfacesPtr, interfaces.size,
                methodsPtr, methods.size,
                fieldsPtr, if (classDesc.isInterface) -1 else fields.size,
                reflectionInfo.packageName,
                reflectionInfo.relativeName,
                flagsFromClass(classDesc),
                makeExtendedInfo(classDesc),
                llvmDeclarations.writableTypeInfoGlobal?.pointer
        )

        val typeInfoGlobalValue = if (!classDesc.typeInfoHasVtableAttached) {
            typeInfo
        } else {
            val vtable = vtable(classDesc)
            Struct(typeInfo, vtable)
        }

        typeInfoGlobal.setInitializer(typeInfoGlobalValue)
        typeInfoGlobal.setConstant(true)

        exportTypeInfoIfRequired(classDesc, classDesc.llvmTypeInfoPtr)
    }

    fun vtable(classDesc: ClassDescriptor): ConstArray {
        // TODO: compile-time resolution limits binary compatibility
        val vtableEntries = context.getVtableBuilder(classDesc).vtableEntries.map {
            val implementation = it.implementation
            if (implementation == null || implementation.isExternalObjCClassMethod()) {
                NullPointer(int8Type)
            } else {
                implementation.entryPointAddress
            }
        }
        return ConstArray(int8TypePtr, vtableEntries)
    }

    fun methodTableRecords(classDesc: ClassDescriptor): List<MethodTableRecord> {
        val functionNames = mutableMapOf<Long, OverriddenFunctionDescriptor>()
        return context.getVtableBuilder(classDesc).methodTableEntries.map {
            val functionName = it.overriddenDescriptor.functionName
            val nameSignature = functionName.localHash
            val previous = functionNames.putIfAbsent(nameSignature.value, it)
            if (previous != null)
                throw AssertionError("Duplicate method table entry: functionName = '$functionName', hash = '${nameSignature.value}', entry1 = $previous, entry2 = $it")

            // TODO: compile-time resolution limits binary compatibility
            val implementation = it.implementation
            val methodEntryPoint = implementation?.entryPointAddress
            MethodTableRecord(nameSignature, methodEntryPoint)
        }.sortedBy { it.nameSignature.value }
    }

    private fun mapRuntimeType(type: LLVMTypeRef): Int =
            runtimeTypeMap[type] ?: throw Error("Unmapped type: ${llvmtype2string(type)}")

    private fun makeExtendedInfo(descriptor: ClassDescriptor): ConstPointer {
        // TODO: shall we actually do that?
        if (context.shouldOptimize())
            return NullPointer(runtime.extendedTypeInfoType)

        val className = descriptor.fqNameSafe.toString()
        val llvmDeclarations = context.llvmDeclarations.forClass(descriptor)
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
            val fields = getStructElements(bodyType).mapIndexedNotNull { index, type ->
                FieldRecord(
                        LLVMOffsetOfElement(llvmTargetData, bodyType, index).toInt(),
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
        return result.pointer
    }

    // TODO: extract more code common with generate().
    fun generateSyntheticInterfaceImpl(
            descriptor: ClassDescriptor,
            methodImpls: Map<FunctionDescriptor, ConstPointer>
    ): ConstPointer {
        assert(descriptor.isInterface)

        val name = "".globalHash

        val size = 0

        val superClass = context.ir.symbols.any.owner

        assert(superClass.implementedInterfaces.isEmpty())
        val interfaces = listOf(descriptor.typeInfoPtr)
        val interfacesPtr = staticData.placeGlobalConstArray("",
                pointerType(runtime.typeInfoType), interfaces)

        assert(superClass.declarations.all { it !is IrProperty && it !is IrField })
        val objOffsetsPtr = NullPointer(int32Type)
        val objOffsetsCount = 0
        val fieldsPtr = NullPointer(runtime.fieldTableRecordType)
        val fieldsCount = 0

        val methods = (methodTableRecords(superClass) + methodImpls.map { (method, impl) ->
            assert(method.containingDeclaration == descriptor)
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
                name = name,
                size = size,
                superType = superClass.typeInfoPtr,
                objOffsets = objOffsetsPtr, objOffsetsCount = objOffsetsCount,
                interfaces = interfacesPtr, interfacesCount = interfaces.size,
                methods = methodsPtr, methodsCount = methods.size,
                fields = fieldsPtr, fieldsCount = fieldsCount,
                packageName = reflectionInfo.packageName,
                relativeName = reflectionInfo.relativeName,
                flags = flagsFromClass(descriptor),
                extendedInfo = NullPointer(runtime.extendedTypeInfoType),
                writableTypeInfo = writableTypeInfo
              ), vtable)

        typeInfoWithVtableGlobal.setInitializer(typeInfoWithVtable)
        typeInfoWithVtableGlobal.setConstant(true)

        return result
    }

    private val OverriddenFunctionDescriptor.implementation get() = getImplementation(context)

    data class ReflectionInfo(val packageName: String?, val relativeName: String?)

    private fun getReflectionInfo(descriptor: ClassDescriptor): ReflectionInfo {
        // Use data from value class in type info for box class:
        val descriptorForReflection = context.ir.symbols.valueClassToBox.entries
                .firstOrNull { it.value.owner == descriptor }
                ?.key?.owner ?: descriptor

        return if (descriptorForReflection.isAnonymousObject) {
            ReflectionInfo(packageName = null, relativeName = null)
        } else if (descriptorForReflection.isLocal) {
            ReflectionInfo(packageName = null, relativeName = descriptorForReflection.name.asString())
        } else {
            ReflectionInfo(
                    packageName = descriptorForReflection.findPackage().fqName.asString(),
                    relativeName = generateSequence(descriptorForReflection, { it.parent as? ClassDescriptor })
                            .toList().reversed()
                            .joinToString(".") { it.name.asString() }
            )
        }
    }
}
