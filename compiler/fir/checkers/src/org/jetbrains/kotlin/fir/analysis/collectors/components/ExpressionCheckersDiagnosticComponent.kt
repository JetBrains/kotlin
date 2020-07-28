/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.*

class ExpressionCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    private val checkers = session.checkersComponent.expressionCheckers

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(typeOperatorCall, data, it) }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(constExpression, data, it) }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(annotationCall, data, it) }
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        runCheck { checkers.qualifiedAccessCheckers.check(qualifiedAccessExpression, data, it) }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        runCheck { checkers.functionCallCheckers.check(functionCall, data, it) }
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        runCheck { checkers.qualifiedAccessCheckers.check(callableReferenceAccess, data, it) }
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(thisReceiverExpression, data, it) }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(resolvedQualifier, data, it) }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(whenExpression, data, it) }
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(binaryLogicExpression, data, it) }
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(arrayOfCall, data, it) }
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(stringConcatenationCall, data, it) }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(checkNotNullCall, data, it) }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(tryExpression, data, it) }
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(classReferenceExpression, data, it) }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(getClassCall, data, it) }
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: CheckerContext) {
        runCheck { checkers.expressionCheckers.check(equalityOperatorCall, data, it) }
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
        runCheck { checkers.variableAssignmentCheckers.check(variableAssignment, data, it) }
    }

    private fun <E : FirStatement> List<FirExpressionChecker<E>>.check(expression: E, context: CheckerContext, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(expression, context, reporter)
        }
    }
}