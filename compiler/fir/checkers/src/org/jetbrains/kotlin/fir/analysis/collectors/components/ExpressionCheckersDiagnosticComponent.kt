/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.call.CallCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.call.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.*

class ExpressionCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(typeOperatorCall, it) }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>) {
        runCheck { CallCheckers.EXPRESSIONS.check(constExpression, it) }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(annotationCall, it) }
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        runCheck { CallCheckers.QUALIFIED_ACCESS.check(qualifiedAccessExpression, it) }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        runCheck { CallCheckers.FUNCTION_CALLS.check(functionCall, it) }
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        runCheck { CallCheckers.QUALIFIED_ACCESS.check(callableReferenceAccess, it) }
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression) {
        runCheck { CallCheckers.EXPRESSIONS.check(thisReceiverExpression, it) }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        runCheck { CallCheckers.EXPRESSIONS.check(resolvedQualifier, it) }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression) {
        runCheck { CallCheckers.EXPRESSIONS.check(whenExpression, it) }
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        runCheck { CallCheckers.EXPRESSIONS.check(binaryLogicExpression, it) }
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(arrayOfCall, it) }
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(stringConcatenationCall, it) }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(checkNotNullCall, it) }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression) {
        runCheck { CallCheckers.EXPRESSIONS.check(tryExpression, it) }
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression) {
        runCheck { CallCheckers.EXPRESSIONS.check(classReferenceExpression, it) }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall) {
        runCheck { CallCheckers.EXPRESSIONS.check(getClassCall, it) }
    }

    private fun <E : FirExpression> List<FirExpressionChecker<E>>.check(expression: E, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(expression, reporter)
        }
    }
}