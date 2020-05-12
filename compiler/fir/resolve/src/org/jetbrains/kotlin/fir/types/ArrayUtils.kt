/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.StandardClassIds


fun ConeTypeProjection.createArrayOf(nullable: Boolean = false): ConeKotlinType {
    if (this is ConeKotlinTypeProjection) {
        val type = type.lowerBoundIfFlexible()
        if (type is ConeClassLikeType) {
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


fun ConeKotlinType.arrayElementType(session: FirSession): ConeKotlinType? {
    val type = this.lowerBoundIfFlexible()
    if (type !is ConeClassLikeType) return null
    val classId = type.lookupTag.classId
    if (classId == StandardClassIds.Array)
        return (type.typeArguments.first() as ConeKotlinTypeProjection).type
    val elementType = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: StandardClassIds.elementTypeByUnsignedArrayType[classId]
    if (elementType != null) {
        return elementType.constructClassLikeType(emptyArray(), isNullable = false)
    }

    return null
}
