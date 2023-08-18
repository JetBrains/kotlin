/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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

fun ConeKotlinType.arrayElementType(checkUnsignedArrays: Boolean = true): ConeKotlinType? {
    return when (val argument = arrayElementTypeArgument(checkUnsignedArrays)) {
        is ConeKotlinTypeProjection -> argument.type
        else -> null
    }
}

private fun ConeKotlinType.arrayElementTypeArgument(checkUnsignedArrays: Boolean = true): ConeTypeProjection? {
    val type = this.lowerBoundIfFlexible()
    if (type !is ConeClassLikeType) return null
    val classId = type.lookupTag.classId
    if (classId == StandardClassIds.Array) {
        return type.typeArguments.first()
    }
    val elementType = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: runIf(checkUnsignedArrays) {
        StandardClassIds.elementTypeByUnsignedArrayType[classId]
    }
    if (elementType != null) {
        return elementType.constructClassLikeType(emptyArray(), isNullable = false)
    }

    return null
}

fun ConeKotlinType.varargElementType(): ConeKotlinType {
    return this.arrayElementType() ?: this
}

fun ConeKotlinType?.isPotentiallyArray(): Boolean =
    this != null && (this.arrayElementType() != null || this is ConeTypeVariableType)
