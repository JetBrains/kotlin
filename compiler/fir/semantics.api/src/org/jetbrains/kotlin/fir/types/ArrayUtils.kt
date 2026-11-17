/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.StandardClassIds

val ConeKotlinType.isArrayOrPrimitiveArray: Boolean
    get() = arrayElementTypeArgument() != null

fun ConeKotlinType.isArrayOrPrimitiveArray(checkUnsignedArrays: Boolean = true): Boolean {
    return arrayElementTypeArgument(checkUnsignedArrays) != null
}

fun ConeKotlinType.createOutArrayType(nullable: Boolean = false, createPrimitiveArrayType: Boolean = true): ConeKotlinType {
    return ConeKotlinTypeProjectionOut(this).createArrayType(nullable, createPrimitiveArrayType)
}

fun ConeTypeProjection.createArrayType(nullable: Boolean = false, createPrimitiveArrayTypeIfPossible: Boolean = true): ConeClassLikeType {
    if (this is ConeKotlinTypeProjection && createPrimitiveArrayTypeIfPossible) {
        val type = type.lowerBoundIfFlexible()
        if (type is ConeClassLikeType && !type.isMarkedNullable) {
            val classId = type.lookupTag.classId
            val primitiveArrayId =
                StandardClassIds.primitiveArrayTypeByElementType[classId] ?: StandardClassIds.unsignedArrayTypeByElementType[classId]
            if (primitiveArrayId != null) {
                return primitiveArrayId.constructClassLikeType(isMarkedNullable = nullable)
            }
        }
    }

    return StandardClassIds.Array.constructClassLikeType(arrayOf(this), nullable)
}

/**
 * Wraps the type in [dimensions] arrays, as in `Array<Array<String>>` for `String` and two dimensions.
 *
 * No primitive array types are created: `Int` with one dimension is `Array<Int>`, not `IntArray`.
 */
fun ConeKotlinType.withArrayDimensions(dimensions: Int): ConeKotlinType {
    var result = this
    repeat(dimensions) {
        result = result.createArrayType(createPrimitiveArrayTypeIfPossible = false)
    }

    return result
}

fun ConeKotlinType.varargElementType(): ConeKotlinType {
    return this.arrayElementType() ?: this
}
