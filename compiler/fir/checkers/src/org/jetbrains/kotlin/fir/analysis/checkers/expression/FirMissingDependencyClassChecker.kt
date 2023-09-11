/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSingleCandidate
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencyClassChecker
 */
object FirMissingDependencyClassChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeReference = expression.calleeReference

        // To replicate K1 behavior, MISSING_DEPENDENCY_CLASS errors should still be reported on error references with a single candidate.
        // All other callee errors should skip reporting of MISSING_DEPENDENCY_CLASS.
        if (calleeReference.isError() && calleeReference.diagnostic !is ConeDiagnosticWithSingleCandidate) return

        val missingTypes = mutableSetOf<ConeKotlinType>()
        fun consider(type: ConeKotlinType) {
            var hasError = false
            var hasMissingClass = false
            type.forEachClassLikeType {
                when (it) {
                    is ConeErrorType -> hasError = true
                    else -> hasMissingClass = hasMissingClass || it.lookupTag.toSymbol(context.session) == null
                }
            }

            if (hasMissingClass && !hasError) {
                val reportedType = type.withNullability(ConeNullability.NOT_NULL, context.session.typeContext).withArguments(emptyArray())
                missingTypes.add(reportedType)
            }
        }

        val symbol = calleeReference.toResolvedCallableSymbol() ?: return
        consider(symbol.resolvedReturnTypeRef.coneType)
        symbol.resolvedReceiverTypeRef?.coneType?.let(::consider)
        (symbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.forEach { consider(it.resolvedReturnTypeRef.coneType) }

        for (missingType in missingTypes) {
            reporter.reportOn(expression.source, FirErrors.MISSING_DEPENDENCY_CLASS, missingType, context)
        }
    }

    private fun ConeKotlinType.forEachClassLikeType(action: (ConeClassLikeType) -> Unit) {
        when (this) {
            is ConeFlexibleType -> {
                lowerBound.forEachClassLikeType(action)
                upperBound.forEachClassLikeType(action)
            }

            is ConeDefinitelyNotNullType -> original.forEachClassLikeType(action)
            is ConeIntersectionType -> intersectedTypes.forEach { it.forEachClassLikeType(action) }
            is ConeClassLikeType -> action(this)
            else -> {} // Ignore all type parameters.
        }
    }
}
