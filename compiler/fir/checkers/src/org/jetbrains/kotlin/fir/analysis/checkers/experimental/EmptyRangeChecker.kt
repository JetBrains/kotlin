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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.FirEvaluatorResult

object EmptyRangeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(FirExpressionEvaluator.PrivateConstantEvaluatorAPI::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.source?.kind is KtFakeSourceElementKind) return

        val calleeName = expression.calleeReference.name.asString()
        if (calleeName != "rangeTo" && calleeName != "downTo" && calleeName != "until" && calleeName != "rangeUntil") return

        val leftConst = expression.constValueOf(expression.explicitReceiver, context) ?: return
        val rightConst = expression.constValueOf(expression.argumentList.arguments.getOrNull(0), context) ?: return

        val cmp = compareConstants(leftConst, rightConst) ?: return

        val needReport = when (calleeName) {
            "rangeTo" -> cmp > 0
            "downTo" -> cmp < 0
            "until", "rangeUntil" -> cmp >= 0
            else -> false
        }

        if (needReport) {
            reporter.reportOn(expression.source, FirErrors.EMPTY_RANGE)
        }
    }

    @OptIn(FirExpressionEvaluator.PrivateConstantEvaluatorAPI::class)
    private fun FirFunctionCall.constValueOf(expr: FirExpression?, context: CheckerContext): Any? {
        if (expr == null) return null
        val eval = FirExpressionEvaluator.evaluateExpression(expr, context.session)
        val lit = when (eval) {
            is FirEvaluatorResult.Evaluated -> eval.result as? FirLiteralExpression
            else -> null
        }
        return lit?.value ?: (expr as? FirLiteralExpression)?.value
    }

    private fun isFloating(x: Any): Boolean = x is Double || x is Float
    private fun isUnsigned(x: Any): Boolean = x is ULong || x is UInt || x is UShort || x is UByte

    private fun compareConstants(a: Any, b: Any): Int? {
        // Char ranges
        if (a is Char && b is Char) return a.code.compareTo(b.code)

        // Unsigned ranges
        if (isUnsigned(a) && isUnsigned(b)) {
            val la = when (a) {
                is ULong -> a
                is UInt -> a.toULong()
                is UShort -> a.toULong()
                is UByte -> a.toULong()
                else -> return null
            }
            val rb = when (b) {
                is ULong -> b
                is UInt -> b.toULong()
                is UShort -> b.toULong()
                is UByte -> b.toULong()
                else -> return null
            }
            return la.compareTo(rb)
        }

        // Floating point ranges (allow mixing with integral signed types)
        if (isFloating(a) || isFloating(b)) {
            if (a is Char || b is Char || isUnsigned(a) || isUnsigned(b)) return null
            val da = when (a) {
                is Double -> a
                is Float -> a.toDouble()
                is Number -> a.toDouble()
                else -> return null
            }
            val db = when (b) {
                is Double -> b
                is Float -> b.toDouble()
                is Number -> b.toDouble()
                else -> return null
            }
            return da.compareTo(db)
        }

        // Signed integral (and boolean/string are not expected here)
        if (a is Number && b is Number) {
            val la = a.toLong()
            val lb = b.toLong()
            return la.compareTo(lb)
        }

        return null
    }
}
