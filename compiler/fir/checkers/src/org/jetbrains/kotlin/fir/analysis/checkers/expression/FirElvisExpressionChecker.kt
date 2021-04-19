/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullLiteral

object FirElvisExpressionChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirElvisExpression) return
        // If the overall expression is not resolved/completed, the corresponding error will be reported separately.
        // See [FirControlFlowStatementsResolveTransformer#transformElvisExpression],
        // where an error type is recorded as the expression's return type.
        if (expression.typeRef.coneType is ConeKotlinErrorType) return

        val lhsType = expression.lhs.typeRef.coneType
        if (lhsType is ConeKotlinErrorType) return
        if (!lhsType.canBeNull) {
            reporter.reportOn(expression.source, FirErrors.USELESS_ELVIS, lhsType, context)
            return
        }

        if (expression.rhs.isNullLiteral) {
            reporter.reportOn(expression.source, FirErrors.USELESS_ELVIS_RIGHT_IS_NULL, context)
        }
    }
}
