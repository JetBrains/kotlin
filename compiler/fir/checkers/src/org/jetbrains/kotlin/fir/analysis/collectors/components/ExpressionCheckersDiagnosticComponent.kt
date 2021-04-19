/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.*

@OptIn(CheckersComponentInternal::class)
class ExpressionCheckersDiagnosticComponent(
    collector: AbstractDiagnosticCollector,
    private val checkers: ExpressionCheckers = collector.session.checkersComponent.expressionCheckers,
) : AbstractDiagnosticCollectorComponent(collector) {

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(anonymousFunction, data, reporter)
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(typeOperatorCall, data, reporter)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(constExpression, data, reporter)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        checkers.allAnnotationCallCheckers.check(annotationCall, data, reporter)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        checkers.allQualifiedAccessCheckers.check(qualifiedAccessExpression, data, reporter)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        checkers.allFunctionCallCheckers.check(functionCall, data, reporter)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        checkers.allQualifiedAccessCheckers.check(callableReferenceAccess, data, reporter)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(thisReceiverExpression, data, reporter)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(resolvedQualifier, data, reporter)
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        checkers.allWhenExpressionCheckers.check(whenExpression, data, reporter)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(binaryLogicExpression, data, reporter)
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(arrayOfCall, data, reporter)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(stringConcatenationCall, data, reporter)
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(checkNotNullCall, data, reporter)
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(elvisExpression, data, reporter)
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: CheckerContext) {
        checkers.basicExpressionCheckers.check(safeCallExpression, data, reporter)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        checkers.allTryExpressionCheckers.check(tryExpression, data, reporter)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(classReferenceExpression, data, reporter)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(getClassCall, data, reporter)
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(equalityOperatorCall, data, reporter)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
        checkers.allVariableAssignmentCheckers.check(variableAssignment, data, reporter)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: CheckerContext) {
        checkers.allReturnExpressionCheckers.check(returnExpression, data, reporter)
    }

    override fun visitBlock(block: FirBlock, data: CheckerContext) {
        checkers.allBlockCheckers.check(block, data, reporter)
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
