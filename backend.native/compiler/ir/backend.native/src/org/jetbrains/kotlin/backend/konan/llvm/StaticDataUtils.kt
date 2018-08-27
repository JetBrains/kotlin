/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*


/**
 * Creates const array-typed global with given name and value.
 * Returns pointer to the first element of the array.
 *
 * If [elements] is empty, then null pointer is returned.
 */
internal fun StaticData.placeGlobalConstArray(name: String,
                                              elemType: LLVMTypeRef,
                                              elements: List<ConstValue>,
                                              isExported: Boolean = false): ConstPointer {
    if (elements.isNotEmpty() || isExported) {
        val global = this.placeGlobalArray(name, elemType, elements, isExported)
        global.setConstant(true)
        return global.pointer.getElementPtr(0)
    } else {
        return NullPointer(elemType)
    }
}

internal fun StaticData.createAlias(name: String, aliasee: ConstPointer): ConstPointer {
    val alias = LLVMAddAlias(context.llvmModule, aliasee.llvmType, aliasee.llvm, name)!!
    return constPointer(alias)
}

internal fun StaticData.placeCStringLiteral(value: String): ConstPointer {
    val chars = value.toByteArray(Charsets.UTF_8).map { Int8(it) } + Int8(0)

    return placeGlobalConstArray("", int8Type, chars)
}