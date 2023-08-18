/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun createExpectActualTypeParameterSubstitutor(
    expectedTypeParameters: List<FirTypeParameterSymbol>,
    actualTypeParameters: List<FirTypeParameterSymbol>,
    useSiteSession: FirSession,
    parentSubstitutor: ConeSubstitutor? = null
): ConeSubstitutor {
    val substitution = expectedTypeParameters.zip(actualTypeParameters).associate { (expectedParameterSymbol, actualParameterSymbol) ->
        expectedParameterSymbol to actualParameterSymbol.toLookupTag().constructType(emptyArray(), isNullable = false)
    }
    val substitutor = ConeSubstitutorByMap(
        substitution,
        useSiteSession
    )
    if (parentSubstitutor == null) {
        return substitutor
    }
    return substitutor.chain(parentSubstitutor)
}

fun areCompatibleExpectActualTypes(
    expectedType: ConeKotlinType?,
    actualType: ConeKotlinType?,
    actualSession: FirSession,
    dynamicTypesEqualToAnything: Boolean = true
): Boolean {
    if (expectedType == null) return actualType == null
    if (actualType == null) return false

    if (!dynamicTypesEqualToAnything) {
        val isExpectedDynamic = expectedType is ConeDynamicType
        val isActualDynamic = actualType is ConeDynamicType
        if (isExpectedDynamic && !isActualDynamic || !isExpectedDynamic && isActualDynamic) {
            return false
        }
    }

    return AbstractTypeChecker.equalTypes(
        actualSession.typeContext,
        expectedType,
        actualType
    )
}