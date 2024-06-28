/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*

/**
 * Collects the upper bounds as [ConeClassLikeType].
 */
fun ConeKotlinType?.collectUpperBounds(): Set<ConeClassLikeType> {
    if (this == null) return emptySet()

    val upperBounds = mutableSetOf<ConeClassLikeType>()
    val seen = mutableSetOf<ConeKotlinType>()
    fun collect(type: ConeKotlinType) {
        if (!seen.add(type)) return // Avoid infinite recursion.

        when (type) {
            is ConeErrorType -> return // Ignore error types
            is ConeLookupTagBasedType -> when (type) {
                is ConeClassLikeType -> upperBounds.add(type)
                is ConeTypeParameterType -> {
                    val symbol = type.lookupTag.typeParameterSymbol
                    symbol.resolvedBounds.forEach { collect(it.coneType) }
                }
                else -> error("missing branch for ${javaClass.name}")
            }
            is ConeTypeVariableType -> {
                val symbol = (type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol ?: return
                symbol.resolvedBounds.forEach { collect(it.coneType) }
            }
            is ConeDefinitelyNotNullType -> collect(type.original)
            is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
            is ConeFlexibleType -> collect(type.upperBound)
            is ConeCapturedType -> type.constructor.supertypes?.forEach(::collect)
            is ConeIntegerConstantOperatorType -> upperBounds.add(type.getApproximatedType())
            is ConeStubType, is ConeIntegerLiteralConstantType -> {
                error("$type should not reach here")
            }
        }
    }

    collect(this)
    return upperBounds
}