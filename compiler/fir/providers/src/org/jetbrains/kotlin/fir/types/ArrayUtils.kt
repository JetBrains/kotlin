/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.StandardClassIds

val ConeKotlinType.isArrayOrPrimitiveArray: Boolean
    get() = arrayElementTypeArgument() != null

fun ConeKotlinType.isArrayOrPrimitiveArray(checkUnsignedArrays: Boolean): Boolean {
    return arrayElementTypeArgument(checkUnsignedArrays) != null
}

fun ConeKotlinType.createOutArrayType(nullable: Boolean = false, createPrimitiveArrayType: Boolean = true): ConeKotlinType {
    return ConeKotlinTypeProjectionOut(this).createArrayType(nullable, createPrimitiveArrayType)
}

fun ConeTypeProjection.createArrayType(nullable: Boolean = false, createPrimitiveArrayTypeIfPossible: Boolean = true): ConeClassLikeType {
    if (this is ConeKotlinTypeProjection && createPrimitiveArrayTypeIfPossible) {
        val type = type.lowerBoundIfFlexible()
        if (type is ConeClassLikeType && type.nullability != ConeNullability.NULLABLE) {
            val classId = type.lookupTag.classId
            val primitiveArrayId =
                StandardClassIds.primitiveArrayTypeByElementType[classId] ?: StandardClassIds.unsignedArrayTypeByElementType[classId]
            if (primitiveArrayId != null) {
                return primitiveArrayId.constructClassLikeType(emptyArray(), nullable)
            }
        }
    }

    return StandardClassIds.Array.constructClassLikeType(arrayOf(this), nullable)
}

fun ConeKotlinType.varargElementType(): ConeKotlinType {
    return this.arrayElementType() ?: this
}

fun ConeKotlinType?.isPotentiallyArray(): Boolean =
    this != null && (this.arrayElementType() != null || this is ConeTypeVariableType)
