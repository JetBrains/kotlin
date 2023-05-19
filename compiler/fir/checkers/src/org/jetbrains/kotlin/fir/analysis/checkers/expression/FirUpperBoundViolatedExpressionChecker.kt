/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.FirTypeRefSource
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.withSource
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isTypeAliasedConstructor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
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
        val calleeSymbol = when (val calleeReference = expression.calleeReference) {
            is FirResolvedErrorReference -> {
                if (calleeReference.diagnostic is ConeInapplicableWrongReceiver) {
                    return
                }
                calleeReference.toResolvedCallableSymbol()
            }
            is FirResolvedNamedReference -> calleeReference.toResolvedCallableSymbol()
            else -> null
        }

        val typeArguments: List<ConeTypeProjection>
        val typeParameters: List<FirTypeParameterSymbol>

        if (calleeSymbol is FirConstructorSymbol && calleeSymbol.isTypeAliasedConstructor) {
            val constructedType = expression.typeRef.coneType.fullyExpandedType(context.session)
            // Updating arguments with source information after expanding the type seems extremely brittle as it relies on identity equality
            // of the expression type arguments and the expanded type arguments. This cannot be applied before expanding the type because it
            // seems like the type is already expended.
            typeArguments = constructedType.typeArguments.map {
                it.withSourceRecursive(expression)
            }

            typeParameters = (constructedType.toSymbol(context.session) as? FirRegularClassSymbol)?.typeParameterSymbols ?: return
        } else {
            typeArguments = expression.typeArguments.map { firTypeProjection ->
                firTypeProjection.toConeTypeProjection().withSource(
                    FirTypeRefSource((firTypeProjection as? FirTypeProjectionWithVariance)?.typeRef, firTypeProjection.source)
                )
            }
            typeParameters = calleeSymbol?.typeParameterSymbols ?: return
        }

        // Neither common calls nor type alias constructor calls may contain projections
        // That should be checked somewhere else
        if (typeArguments.any { it !is ConeKotlinType }) {
            return
        }

        if (typeArguments.size != typeParameters.size) return

        val substitutor = substitutorByMap(
            typeParameters.withIndex().associate { Pair(it.value, typeArguments[it.index] as ConeKotlinType) },
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

    private fun ConeTypeProjection.withSourceRecursive(expression: FirQualifiedAccessExpression): ConeTypeProjection {
        // Recursively apply source to any arguments of this type.
        val type = when {
            this is ConeClassLikeType && typeArguments.isNotEmpty() -> withArguments { it.withSourceRecursive(expression) }
            else -> this
        }

        // Try to match the expanded type arguments back to the original expression type arguments.
        return when (val argument = expression.typeArguments.find { it.toConeTypeProjection() === this }) {
            // Unable to find a matching argument, fall back to marking the entire expression.
            null -> type.withSource(FirTypeRefSource(null, expression.source))
            // Found the original argument!
            else -> type.withSource(FirTypeRefSource((argument as? FirTypeProjectionWithVariance)?.typeRef, argument.source))
        }
    }
}
