/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall

object EmptyRangeChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source is FirFakeSourceElement<*>) return
        if (expression !is FirFunctionCall) return
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
            reporter.report(expression.source, FirErrors.EMPTY_RANGE)

        }
    }

    private val FirFunctionCall.rangeLeft: Long?
        get() {
            return if (explicitReceiver is FirIntegerOperatorCall) {
                (explicitReceiver as? FirIntegerOperatorCall)?.asLong
            } else {
                (explicitReceiver as? FirConstExpression<*>)?.value as? Long
            }
        }

    private val FirFunctionCall.rangeRight: Long?
        get() {
            val arg = argumentList.arguments.getOrNull(0)
            return if (arg is FirIntegerOperatorCall) arg.asLong
            else (arg as? FirConstExpression<*>)?.value as? Long
        }

    // todo: add proper integer operator calls checking (e.g. (1+2)*3 transforms to 9)
    private val FirIntegerOperatorCall.asLong: Long?
        get() {
            val value = (dispatchReceiver as? FirConstExpression<*>)?.value as Long? ?: return null
            if (this.calleeReference.name.asString() == "unaryMinus") {
                return -value
            }
            return value
        }
}
