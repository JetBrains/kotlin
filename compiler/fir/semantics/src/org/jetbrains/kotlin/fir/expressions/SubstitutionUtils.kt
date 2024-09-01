/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType

fun FirQualifiedAccessExpression.createConeSubstitutorFromTypeArguments(
    session: FirSession,
    discardErrorTypes: Boolean = false,
): ConeSubstitutor? {
    val symbol = calleeReference.toResolvedCallableSymbol() ?: return null
    return createConeSubstitutorFromTypeArguments(symbol, session, discardErrorTypes)
}

/**
 * @param discardErrorTypes if true, then type arguments with error types are not added to the substitution map
 */
fun FirQualifiedAccessExpression.createConeSubstitutorFromTypeArguments(
    callableSymbol: FirCallableSymbol<*>,
    session: FirSession,
    discardErrorTypes: Boolean = false,
    // TODO: Get rid of this parameter once KT-59138 is fixed and the relevant feature for disabling it will be removed
    unwrapExplicitTypeArgumentForMadeFlexibleSynthetically: Boolean = false,
): ConeSubstitutor {
    val typeArgumentMap = buildMap {
        // Type arguments are ignored defensively if `callableSymbol` can't provide enough type parameters (and vice versa). For
        // example, when call candidates are collected, the candidate's `callableSymbol` might have fewer type parameters than the
        // inferred call's type arguments.
        typeArguments.zip(callableSymbol.typeParameterSymbols).forEach { (typeArgument, typeParameterSymbol) ->
            val type = (typeArgument as? FirTypeProjectionWithVariance)?.typeRef?.coneType ?: return@forEach
            if (type is ConeErrorType && discardErrorTypes) return@forEach

            val resultingType = when {
                unwrapExplicitTypeArgumentForMadeFlexibleSynthetically ->
                    type.attributes.explicitTypeArgumentIfMadeFlexibleSynthetically?.coneType ?: type
                else -> type
            }

            put(typeParameterSymbol, resultingType)
        }
    }
    return substitutorByMap(typeArgumentMap, session)
}
