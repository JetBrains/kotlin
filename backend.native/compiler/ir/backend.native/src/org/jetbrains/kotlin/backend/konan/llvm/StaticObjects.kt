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

import llvm.LLVMLinkage
import llvm.LLVMSetLinkage
import llvm.LLVMTypeRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.descriptors.isUnit
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.replace

private fun StaticData.objHeader(typeInfo: ConstPointer): Struct {
    val containerOffsetNegative = 0 // Static object mark.
    return Struct(runtime.objHeaderType, typeInfo, Int32(containerOffsetNegative))
}

private fun StaticData.arrayHeader(typeInfo: ConstPointer, length: Int): Struct {
    assert (length >= 0)
    val containerOffsetNegative = 0 // Static object mark.
    return Struct(runtime.arrayHeaderType, typeInfo, Int32(containerOffsetNegative), Int32(length))
}

internal fun StaticData.createKotlinStringLiteral(type: KotlinType, irConst: IrConst<String>): ConstPointer {
    val value = irConst.value
    val name = "kstr:" + value.globalHashBase64
    val elements = value.toCharArray().map(::Char16)

    val objRef = createKotlinArray(type, elements)

    val res = createAlias(name, objRef)
    LLVMSetLinkage(res.llvm, LLVMLinkage.LLVMWeakAnyLinkage)

    return res
}

private fun StaticData.createRef(type: KotlinType, objHeaderPtr: ConstPointer): ConstPointer {
    val llvmType = getLLVMType(type)
    return if (llvmType != objHeaderPtr.llvmType) {
        objHeaderPtr.bitcast(llvmType)
    } else {
        objHeaderPtr
    }
}

internal fun StaticData.createKotlinArray(arrayType: KotlinType, elements: List<LLVMValueRef>) =
        createKotlinArray(arrayType, elements.map { constValue(it) }).llvm

internal fun StaticData.createKotlinArray(arrayType: KotlinType, elements: List<ConstValue>): ConstPointer {

    val typeInfo = arrayType.typeInfoPtr!!

    val bodyElementType: LLVMTypeRef = elements.firstOrNull()?.llvmType ?: int8Type
    // (use [0 x i8] as body if there are no elements)
    val arrayBody = ConstArray(bodyElementType, elements)

    val compositeType = structType(runtime.arrayHeaderType, arrayBody.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val arrayHeader = arrayHeader(typeInfo, elements.size)

    global.setInitializer(Struct(compositeType, arrayHeader, arrayBody))

    return createRef(arrayType, objHeaderPtr)
}

internal fun StaticData.createKotlinObject(type: KotlinType, body: ConstValue): ConstPointer {
    val typeInfo = type.typeInfoPtr!!

    val compositeType = structType(runtime.objHeaderType, body.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val objHeader = objHeader(typeInfo)

    global.setInitializer(Struct(compositeType, objHeader, body))

    return createRef(type, objHeaderPtr)
}

private fun StaticData.getArrayListClass(): ClassDescriptor {
    val module = context.irModule!!.descriptor
    val pkg = module.getPackage(FqName.fromSegments(listOf("kotlin", "collections")))
    val classifier = pkg.memberScope.getContributedClassifier(Name.identifier("ArrayList"),
            NoLookupLocation.FROM_BACKEND)

    return classifier as ClassDescriptor
}

/**
 * Creates static instance of `kotlin.collections.ArrayList<elementType>` with given values of fields.
 *
 * @param array value for `array: Array<E>` field.
 * @param length value for `length: Int` field.
 */
internal fun StaticData.createArrayList(elementType: TypeProjection, array: ConstPointer, length: Int): ConstPointer {
    val arrayListClass = getArrayListClass()

    // type is ArrayList<elementType>:
    val type = arrayListClass.defaultType.replace(listOf(elementType))

    val arrayListFqName = arrayListClass.fqNameSafe
    val arrayListFields = mapOf(
        "$arrayListFqName.array" to array,
        "$arrayListFqName.offset" to Int32(0),
        "$arrayListFqName.length" to Int32(length),
        "$arrayListFqName.backing" to NullPointer(kObjHeader))

    // Now sort these values according to the order of fields returned by getFields()
    // to match the sorting order of the real ArrayList().
    val sorted = linkedMapOf<String, ConstValue>()
    getFields(arrayListClass).forEach {
        val fqName = it.fqNameSafe.asString()
        sorted.put(fqName, arrayListFields[fqName]!!)
    }
        
    val body = Struct(*(sorted.values.toTypedArray()))

    return createKotlinObject(type, body)
}

internal fun StaticData.createUnitInstance(descriptor: ClassDescriptor,
                                           bodyType: LLVMTypeRef,
                                           typeInfo: ConstPointer
): ConstPointer {
    assert (descriptor.isUnit())
    assert (getStructElements(bodyType).isEmpty())
    val objHeader = objHeader(typeInfo)
    val global = this.placeGlobal(theUnitInstanceName, objHeader, isExported = true)
    return global.pointer
}

internal val ContextUtils.theUnitInstanceRef: ConstPointer
    get() {
        val unitDescriptor = context.builtIns.unit
        return if (isExternal(unitDescriptor)) {
            constPointer(importGlobal(theUnitInstanceName, context.llvm.runtime.objHeaderType))
        } else {
            context.llvmDeclarations.getUnitInstanceRef()
        }
    }
