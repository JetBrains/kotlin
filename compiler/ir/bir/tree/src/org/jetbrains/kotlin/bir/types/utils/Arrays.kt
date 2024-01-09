/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirStarProjection
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.BirTypeProjection
import org.jetbrains.kotlin.bir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.builtins.StandardNames

val BirType.isBoxedArray: Boolean
    get() = classOrNull?.owner?.fqNameWhenAvailable == StandardNames.FqNames.array.toSafe()

fun BirType.getArrayElementType(irBuiltIns: BirBuiltIns): BirType =
    if (isBoxedArray) {
        when (val argument = (this as BirSimpleType).arguments.singleOrNull()) {
            is BirTypeProjection ->
                argument.type
            is BirStarProjection ->
                irBuiltIns.anyNType
            null ->
                error("Unexpected array argument type: null")
        }
    } else {
        val classifier = this.classOrNull!!
        irBuiltIns.primitiveArrayElementTypes[classifier]
            ?: irBuiltIns.unsignedArraysElementTypes[classifier]
            ?: throw AssertionError("Primitive array expected: $classifier")
    }

fun BirType.toArrayOrPrimitiveArrayType(irBuiltIns: BirBuiltIns): BirType =
    if (isPrimitiveType()) {
        irBuiltIns.primitiveArrayForType[this]?.defaultType
            ?: throw AssertionError("$this not in primitiveArrayForType")
    } else {
        irBuiltIns.arrayClass.createType(this)
    }
