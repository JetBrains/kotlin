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
import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.backend.konan.irasdescriptors.llvmSymbolOrigin
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


private fun StaticData.objHeader(typeInfo: ConstPointer): Struct {
    val container = constValue(context.llvm.staticContainer)
    return Struct(runtime.objHeaderType, typeInfo, container)
}

private fun StaticData.arrayHeader(typeInfo: ConstPointer, length: Int): Struct {
    assert (length >= 0)
    val container = constValue(context.llvm.staticContainer)
    return Struct(runtime.arrayHeaderType, typeInfo, container, Int32(length))
}

internal fun StaticData.createKotlinStringLiteral(value: String): ConstPointer {
    val name = "kstr:" + value.globalHashBase64
    val elements = value.toCharArray().map(::Char16)

    val objRef = createKotlinArray(context.ir.symbols.string.owner, elements)

    val res = createAlias(name, objRef)
    LLVMSetLinkage(res.llvm, LLVMLinkage.LLVMWeakAnyLinkage)

    return res
}

private fun StaticData.createRef(objHeaderPtr: ConstPointer) = objHeaderPtr.bitcast(kObjHeaderPtr)

internal fun StaticData.createKotlinArray(arrayClass: IrClass, elements: List<LLVMValueRef>) =
        createKotlinArray(arrayClass, elements.map { constValue(it) }).llvm

internal fun StaticData.createKotlinArray(arrayClass: IrClass, elements: List<ConstValue>): ConstPointer {
    val typeInfo = arrayClass.typeInfoPtr

    val bodyElementType: LLVMTypeRef = elements.firstOrNull()?.llvmType ?: int8Type
    // (use [0 x i8] as body if there are no elements)
    val arrayBody = ConstArray(bodyElementType, elements)

    val compositeType = structType(runtime.arrayHeaderType, arrayBody.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val arrayHeader = arrayHeader(typeInfo, elements.size)

    global.setInitializer(Struct(compositeType, arrayHeader, arrayBody))

    return createRef(objHeaderPtr)
}

internal fun StaticData.createKotlinObject(type: IrClass, body: ConstValue): ConstPointer {
    val typeInfo = type.typeInfoPtr

    val compositeType = structType(runtime.objHeaderType, body.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val objHeader = objHeader(typeInfo)

    global.setInitializer(Struct(compositeType, objHeader, body))

    return createRef(objHeaderPtr)
}

internal fun StaticData.createInitializer(type: IrClass, body: ConstValue): ConstValue =
        Struct(objHeader(type.typeInfoPtr), body)

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
internal fun StaticData.createArrayList(array: ConstPointer, length: Int): ConstPointer {
    val arrayListClass = context.ir.symbols.arrayList.owner

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

    return createKotlinObject(arrayListClass, body)
}

internal fun StaticData.createUniqueInstance(
        kind: UniqueKind, bodyType: LLVMTypeRef, typeInfo: ConstPointer): ConstPointer {
    assert (getStructElements(bodyType).isEmpty())
    val objHeader = when (kind) {
        UniqueKind.UNIT -> objHeader(typeInfo)
        UniqueKind.EMPTY_ARRAY -> arrayHeader(typeInfo, 0)
    }
    val global = this.placeGlobal(kind.llvmName, objHeader, isExported = true)
    return global.pointer
}

internal fun ContextUtils.unique(kind: UniqueKind): ConstPointer {
    val descriptor = when (kind) {
        UniqueKind.UNIT -> context.ir.symbols.unit.owner
        UniqueKind.EMPTY_ARRAY -> context.ir.symbols.array.owner
    }
    return if (isExternal(descriptor)) {
        constPointer(importGlobal(
                kind.llvmName, context.llvm.runtime.objHeaderType, origin = descriptor.llvmSymbolOrigin
        ))
    } else {
        context.llvmDeclarations.forUnique(kind).pointer
    }
}

internal val ContextUtils.theUnitInstanceRef: ConstPointer
    get() = this.unique(UniqueKind.UNIT)