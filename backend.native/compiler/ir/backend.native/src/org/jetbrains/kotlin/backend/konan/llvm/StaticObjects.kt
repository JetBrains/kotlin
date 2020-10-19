/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.cValuesOf
import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.ir.declarations.IrClass

private fun ConstPointer.add(index: Int): ConstPointer {
    return constPointer(LLVMConstGEP(llvm, cValuesOf(Int32(index).llvm), 1)!!)
}

// Must match OBJECT_TAG_PERMANENT_CONTAINER in C++.
private fun StaticData.permanentTag(typeInfo: ConstPointer): ConstPointer {
    // Only pointer arithmetic via GEP works on constant pointers in LLVM.
    return typeInfo.bitcast(int8TypePtr).add(1).bitcast(kTypeInfoPtr)
}

private fun StaticData.objHeader(typeInfo: ConstPointer): Struct {
    return Struct(runtime.objHeaderType, permanentTag(typeInfo))
}

private fun StaticData.arrayHeader(typeInfo: ConstPointer, length: Int): Struct {
    assert (length >= 0)
    return Struct(runtime.arrayHeaderType, permanentTag(typeInfo), Int32(length))
}

internal fun StaticData.createKotlinStringLiteral(value: String): ConstPointer {
    val elements = value.toCharArray().map(::Char16)
    val objRef = createConstKotlinArray(context.ir.symbols.string.owner, elements)
    return objRef
}

private fun StaticData.createRef(objHeaderPtr: ConstPointer) = objHeaderPtr.bitcast(kObjHeaderPtr)

internal fun StaticData.createConstKotlinArray(arrayClass: IrClass, elements: List<LLVMValueRef>) =
        createConstKotlinArray(arrayClass, elements.map { constValue(it) }).llvm

internal fun StaticData.createConstKotlinArray(arrayClass: IrClass, elements: List<ConstValue>): ConstPointer {
    val typeInfo = arrayClass.typeInfoPtr

    val bodyElementType: LLVMTypeRef = elements.firstOrNull()?.llvmType ?: int8Type
    // (use [0 x i8] as body if there are no elements)
    val arrayBody = ConstArray(bodyElementType, elements)

    val compositeType = structType(runtime.arrayHeaderType, arrayBody.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val arrayHeader = arrayHeader(typeInfo, elements.size)

    global.setInitializer(Struct(compositeType, arrayHeader, arrayBody))
    global.setConstant(true)

    return createRef(objHeaderPtr)
}

internal fun StaticData.createConstKotlinObject(type: IrClass, vararg fields: ConstValue): ConstPointer {
    val typeInfo = type.typeInfoPtr
    val objHeader = objHeader(typeInfo)

    val global = this.placeGlobal("", Struct(objHeader, *fields))
    global.setConstant(true)

    val objHeaderPtr = global.pointer.getElementPtr(0)

    return createRef(objHeaderPtr)
}

internal fun StaticData.createInitializer(type: IrClass, vararg fields: ConstValue): ConstValue =
        Struct(objHeader(type.typeInfoPtr), *fields)

/**
 * Creates static instance of `kotlin.collections.ArrayList<elementType>` with given values of fields.
 *
 * @param array value for `array: Array<E>` field.
 * @param length value for `length: Int` field.
 */
internal fun StaticData.createConstArrayList(array: ConstPointer, length: Int): ConstPointer {
    val arrayListClass = context.ir.symbols.arrayList.owner

    val arrayListFields = mapOf(
        "array" to array,
        "offset" to Int32(0),
        "length" to Int32(length),
        "backing" to NullPointer(kObjHeader))

    // Now sort these values according to the order of fields returned by getFields()
    // to match the sorting order of the real ArrayList().
    val sorted = mutableListOf<ConstValue>()
    context.getLayoutBuilder(arrayListClass).fields.forEach {
        require (it.parent == arrayListClass)
        sorted.add(arrayListFields[it.name.asString()]!!)
    }

    return createConstKotlinObject(arrayListClass, *sorted.toTypedArray())
}

internal fun StaticData.createUniqueInstance(
        kind: UniqueKind, bodyType: LLVMTypeRef, typeInfo: ConstPointer): ConstPointer {
    assert (getStructElements(bodyType).size == 1) // ObjHeader only.
    val objHeader = when (kind) {
        UniqueKind.UNIT -> objHeader(typeInfo)
        UniqueKind.EMPTY_ARRAY -> arrayHeader(typeInfo, 0)
    }
    val global = this.placeGlobal(kind.llvmName, objHeader, isExported = true)
    global.setConstant(true)
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