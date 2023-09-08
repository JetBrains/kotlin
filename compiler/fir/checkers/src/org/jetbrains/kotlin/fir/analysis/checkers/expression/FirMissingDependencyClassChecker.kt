/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencyClassChecker
 */
object FirMissingDependencyClassChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeReference = expression.calleeReference
        if (calleeReference.isError()) {
            // To replicate K1 behavior, MISSING_DEPENDENCY_CLASS errors should still be reported on unsafe calls.
            // All other callee errors should skip reporting of MISSING_DEPENDENCY_CLASS.
            val diagnostic = calleeReference.diagnostic
            if (diagnostic !is ConeInapplicableCandidateError || diagnostic.applicability != CandidateApplicability.UNSAFE_CALL) {
                return
            }
        }

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
                reporter.reportOn(expression.source, FirErrors.MISSING_DEPENDENCY_CLASS, type, context)
            }
        }

        consider(expression.resolvedType)
        expression.extensionReceiver?.resolvedType?.let(::consider)
        expression.argumentList.arguments.forEach { consider(it.resolvedType) }
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
