/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.isNullLiteral
import org.jetbrains.kotlin.fir.types.resolvedType

object FirUselessElvisChecker : FirElvisExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirElvisExpression) {
        // If the overall expression is not resolved/completed, the corresponding error will be reported separately.
        // See [FirControlFlowStatementsResolveTransformer#transformElvisExpression],
        // where an error type is recorded as the expression's return type.
        if (expression.resolvedType is ConeErrorType) return

        // Check if left side is null literal
        if (expression.lhs.isNullLiteral) {
            if (LanguageFeature.EnableDfaWarningsInK2.isEnabled()) {
                reporter.reportOn(expression.source, FirErrors.USELESS_ELVIS_LEFT_IS_NULL)
            }
            return
        }

        val lhsType = expression.lhs.resolvedType
        if (lhsType is ConeErrorType) return
        if (!lhsType.canBeNull(context.session)) {
            if (LanguageFeature.EnableDfaWarningsInK2.isEnabled()) {
                reporter.reportOn(expression.source, FirErrors.USELESS_ELVIS, lhsType)
            }
            return
        }

        if (expression.rhs.isNullLiteral) {
            if (LanguageFeature.EnableDfaWarningsInK2.isEnabled()) {
                reporter.reportOn(expression.source, FirErrors.USELESS_ELVIS_RIGHT_IS_NULL)
            }
        }
    }
}
