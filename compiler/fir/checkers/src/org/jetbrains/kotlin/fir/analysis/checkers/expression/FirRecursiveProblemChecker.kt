/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.types.*

object FirRecursiveProblemChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            expression !is FirExpression ||
            expression.source?.kind is KtFakeSourceElementKind ||
            expression.isExpressionWithAlwaysImplicitTypeRef
        ) {
            return
        }

        fun checkConeType(coneType: ConeKotlinType?) {
            if (coneType is ConeErrorType && (coneType.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.RecursionInImplicitTypes) {
                reporter.reportOn(expression.source, FirErrors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, context)
            } else if (coneType is ConeClassLikeType) {
                for (typeArgument in coneType.typeArguments) {
                    checkConeType(typeArgument.type)
                }
            }
        }

        checkConeType(expression.resolvedType)
    }

    private val FirStatement.isExpressionWithAlwaysImplicitTypeRef get() = this is FirNoReceiverExpression
}
