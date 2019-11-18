/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.invoke


fun ConeKotlinType.createArrayOf(session: FirSession, nullable: Boolean = false): ConeKotlinType {
    val symbolProvider: FirSymbolProvider = session.firSymbolProvider
    val type = lowerBoundIfFlexible()
    if (type is ConeClassType) {
        val primitiveArrayId = StandardClassIds.primitiveArrayTypeByElementType[type.lookupTag.classId]
        if (primitiveArrayId != null) {
            return primitiveArrayId.invoke(symbolProvider).constructType(emptyArray(), nullable)
        }
    }

    return StandardClassIds.Array.invoke(symbolProvider).constructType(arrayOf(this), nullable)
}


fun ConeKotlinType.arrayElementType(session: FirSession): ConeKotlinType? {
    val type = this.lowerBoundIfFlexible()
    if (type !is ConeClassType) return null
    val classId = type.lookupTag.classId
    if (classId == StandardClassIds.Array)
        return (type.typeArguments.first() as ConeTypedProjection).type
    val elementType = StandardClassIds.elementTypeByPrimitiveArrayType[classId]
    if (elementType != null) {
        return elementType.invoke(session.firSymbolProvider).constructType(emptyArray(), isNullable = false)
    }

    return null
}