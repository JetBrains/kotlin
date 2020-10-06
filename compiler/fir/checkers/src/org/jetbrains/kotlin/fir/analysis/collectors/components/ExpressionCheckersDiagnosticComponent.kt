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
        checkers.basicExpressionCheckers.check(typeOperatorCall, data, reporter)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(constExpression, data, reporter)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(annotationCall, data, reporter)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        checkers.qualifiedAccessCheckers.check(qualifiedAccessExpression, data, reporter)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        checkers.functionCallCheckers.check(functionCall, data, reporter)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        checkers.qualifiedAccessCheckers.check(callableReferenceAccess, data, reporter)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(thisReceiverExpression, data, reporter)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(resolvedQualifier, data, reporter)
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(whenExpression, data, reporter)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(binaryLogicExpression, data, reporter)
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(arrayOfCall, data, reporter)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(stringConcatenationCall, data, reporter)
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(checkNotNullCall, data, reporter)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(tryExpression, data, reporter)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(classReferenceExpression, data, reporter)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(getClassCall, data, reporter)
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(equalityOperatorCall, data, reporter)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
        checkers.variableAssignmentCheckers.check(variableAssignment, data, reporter)
    }

    private fun <E : FirStatement> Collection<FirExpressionChecker<E>>.check(
        expression: E,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (checker in this) {
            checker.check(expression, context, reporter)
        }
    }
}
