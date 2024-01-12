/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

object EmptyRangeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source?.kind is KtFakeSourceElementKind) return
        val left = expression.rangeLeft ?: return
        val right = expression.rangeRight ?: return

        val needReport = when (expression.calleeReference.name.asString()) {
            "rangeTo" -> {
                left > right
            }
            "downTo" -> {
                right > left
            }
            "until" -> {
                left >= right
            }
            else -> false
        }

        if (needReport) {
            reporter.reportOn(expression.source, FirErrors.EMPTY_RANGE, context)
        }
    }

    private val FirFunctionCall.rangeLeft: Long?
        get() {
            return (explicitReceiver as? FirLiteralExpression<*>)?.value as? Long
        }

    private val FirFunctionCall.rangeRight: Long?
        get() {
            val arg = argumentList.arguments.getOrNull(0) as? FirLiteralExpression<*>
            return arg?.value as? Long
        }
}
