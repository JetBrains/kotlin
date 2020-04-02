/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.call.CallCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.call.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.*

class ExpressionCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(typeOperatorCall, data, it) }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(constExpression, data, it) }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(annotationCall, data, it) }
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        runCheck { CallCheckers.QUALIFIED_ACCESS.check(qualifiedAccessExpression, data, it) }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        runCheck { CallCheckers.FUNCTION_CALLS.check(functionCall, data, it) }
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        runCheck { CallCheckers.QUALIFIED_ACCESS.check(callableReferenceAccess, data, it) }
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(thisReceiverExpression, data, it) }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(resolvedQualifier, data, it) }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(whenExpression, data, it) }
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(binaryLogicExpression, data, it) }
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(arrayOfCall, data, it) }
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(stringConcatenationCall, data, it) }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(checkNotNullCall, data, it) }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(tryExpression, data, it) }
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(classReferenceExpression, data, it) }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        runCheck { CallCheckers.EXPRESSIONS.check(getClassCall, data, it) }
    }

    private fun <E : FirExpression> List<FirExpressionChecker<E>>.check(expression: E, context: CheckerContext, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(expression, context, reporter)
        }
    }
}