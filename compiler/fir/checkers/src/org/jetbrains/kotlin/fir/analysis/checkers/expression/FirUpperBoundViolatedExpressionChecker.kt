/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.TypeArgumentWithSourceInfo
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isTypeAliasedConstructor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirUpperBoundViolatedExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeReference = expression.calleeReference
        var calleeSymbol: FirCallableSymbol<*>? = null
        if (calleeReference is FirResolvedNamedReference) {
            calleeSymbol = calleeReference.toResolvedCallableSymbol()
        } else if (calleeReference is FirErrorNamedReference) {
            if (calleeReference.diagnostic is ConeInapplicableWrongReceiver) {
                return
            }
            calleeSymbol = calleeReference.candidateSymbol as? FirCallableSymbol<*>
        }

        val typeArguments: List<TypeArgumentWithSourceInfo>
        val typeParameters: List<FirTypeParameterSymbol>

        if (calleeSymbol is FirConstructorSymbol && calleeSymbol.isTypeAliasedConstructor) {
            val constructedType = expression.typeRef.coneType.fullyExpandedType(context.session)
            typeArguments = constructedType.typeArguments.map {
                TypeArgumentWithSourceInfo(it, typeRef = null, expression.source)
            }

            typeParameters = (constructedType.toSymbol(context.session) as? FirRegularClassSymbol)?.typeParameterSymbols ?: return
        } else {
            typeArguments = expression.typeArguments.map { firTypeProjection ->
                TypeArgumentWithSourceInfo(
                    firTypeProjection.toConeTypeProjection(),
                    (firTypeProjection as? FirTypeProjectionWithVariance)?.typeRef,
                    firTypeProjection.source
                )
            }
            typeParameters = calleeSymbol?.typeParameterSymbols ?: return
        }

        // Neither common calls nor type alias constructor calls may contain projections
        // That should be checked somewhere else
        if (typeArguments.any { it.coneTypeProjection !is ConeKotlinType }) {
            return
        }

        if (typeArguments.size != typeParameters.size) return

        val substitutor = substitutorByMap(
            typeParameters.withIndex().associate { Pair(it.value, typeArguments[it.index].coneTypeProjection as ConeKotlinType) },
            context.session,
        )

        checkUpperBoundViolated(
            context,
            reporter,
            typeParameters,
            typeArguments,
            substitutor,
        )
    }
}
