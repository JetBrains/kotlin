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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue


internal class RTTIGenerator(override val context: Context) : ContextUtils {

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
            val nameValue = annot.allValueArguments.values.single() as StringValue
            // TODO: use LLVMAddAlias?
            val global = addGlobal(nameValue.value, pointerType(runtime.typeInfoType), isExported = true)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            "kotlin.Array"              to -LLVMABISizeOfType(llvmTargetData, kObjHeaderPtr).toInt(),
            "kotlin.ByteArray"          to -1,
            "kotlin.CharArray"          to -2,
            "kotlin.ShortArray"         to -2,
            "kotlin.IntArray"           to -4,
            "kotlin.LongArray"          to -8,
            "kotlin.FloatArray"         to -4,
            "kotlin.DoubleArray"        to -8,
            "kotlin.BooleanArray"       to -1,
            "kotlin.String"             to -2,
            "konan.ImmutableBinaryBlob" to -1
    )

    private fun getInstanceSize(classType: LLVMTypeRef?, className: FqName) : Int {
        val arraySize = arrayClasses.get(className.asString());
        if (arraySize != null) return arraySize;
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
                objOffsetsPtr, objOffsets.size,
                interfacesPtr, interfaces.size,
                methodsPtr, methods.size,
                fieldsPtr, if (classDesc.isInterface) -1 else fields.size,
                reflectionInfo.packageName,
                reflectionInfo.relativeName,
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

    // TODO: extract more code common with generate()
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
