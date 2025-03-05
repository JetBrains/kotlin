/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirUnusedCheckerBase
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*

object FirUnusedExpressionChecker : FirUnusedCheckerBase() {
    override fun isEnabled(context: CheckerContext): Boolean = true // Controlled by FIR_EXTRA_CHECKERS

    override fun reportUnusedExpressionIfNeeded(
        expression: FirExpression,
        hasSideEffects: Boolean,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        source: KtSourceElement?,
    ): Boolean {
        if (hasSideEffects) return false

        val factory = when {
            expression is FirAnonymousFunctionExpression && expression.anonymousFunction.isLambda
                -> FirErrors.UNUSED_LAMBDA_EXPRESSION
            else -> FirErrors.UNUSED_EXPRESSION
        }
        reporter.reportOn(source, factory, context)
        return true
    }
}
