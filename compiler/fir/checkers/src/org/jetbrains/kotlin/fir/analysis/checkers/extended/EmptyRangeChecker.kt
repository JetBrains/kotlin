/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpresionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall

object EmptyRangeChecker : FirBasicExpresionChecker() {
    override fun check(functionCall: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (functionCall.source is FirFakeSourceElement<*>) return
        if (functionCall !is FirFunctionCall) return
        val left = functionCall.rangeLeft ?: return
        val right = functionCall.rangeRight ?: return

        val needReport = when (functionCall.calleeReference.name.asString()) {
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
            reporter.report(functionCall.source, FirErrors.EMPTY_RANGE)

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

    private val FirIntegerOperatorCall.asLong: Long?
        get() {
            val value = (dispatchReceiver as? FirConstExpression<*>)?.value as Long? ?: return null
            if (this.calleeReference.name.asString() == "unaryMinus") {
                return -value
            }
            return value
        }
}