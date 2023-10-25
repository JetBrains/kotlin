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

fun createExpectActualTypeParameterSubstitutor(
    expectActualTypeParameters: List<Pair<FirTypeParameterSymbol, FirTypeParameterSymbol>>,
    useSiteSession: FirSession,
    parentSubstitutor: ConeSubstitutor? = null
): ConeSubstitutor {
    val substitution = expectActualTypeParameters.associate { (expectedParameterSymbol, actualParameterSymbol) ->
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
