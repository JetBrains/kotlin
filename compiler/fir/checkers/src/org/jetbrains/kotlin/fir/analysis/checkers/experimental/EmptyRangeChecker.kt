/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.experimental

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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.source?.kind is KtFakeSourceElementKind) return
        val comparison = expression.compareLeftAndRight() ?: return

        val needReport = when (expression.calleeReference.name.asString()) {
            "rangeTo" -> {
                comparison > 0
            }
            "downTo" -> {
                comparison < 0
            }
            "until", "rangeUntil" -> {
                comparison >= 0
            }
            else -> false
        }

        if (needReport) {
            reporter.reportOn(expression.source, FirErrors.EMPTY_RANGE)
        }
    }

    private fun FirFunctionCall.compareLeftAndRight(): Int? {
        val left = (explicitReceiver as? FirLiteralExpression)?.value ?: return null
        val right = (argumentList.arguments.getOrNull(0) as? FirLiteralExpression)?.value ?: return null
        return when (left) {
            is Long -> (right as? Long)?.let { left.compareTo(it) }
            is Float -> (right as? Float)?.let { left.compareTo(it) }
            is Double -> (right as? Double)?.let { left.compareTo(it) }
            is Char -> (right as? Char)?.let { left.compareTo(it) }
            else -> null
        }
    }
}
