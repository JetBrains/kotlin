/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.*

@OptIn(CheckersComponentInternal::class)
class ExpressionCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: ExpressionCheckers = session.checkersComponent.expressionCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: CheckerContext) {
        checkers.allTypeOperatorCallCheckers.check(typeOperatorCall, data)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        checkers.allConstExpressionCheckers.check(constExpression, data)
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: CheckerContext) {
        checkers.allAnnotationCheckers.check(annotation, data)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        checkers.allAnnotationCallCheckers.check(annotationCall, data)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        checkers.allQualifiedAccessExpressionCheckers.check(qualifiedAccessExpression, data)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: CheckerContext) {
        checkers.allPropertyAccessExpressionCheckers.check(propertyAccessExpression, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        checkers.allFunctionCallCheckers.check(functionCall, data)
    }

    override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: CheckerContext) {
        checkers.allIntegerLiteralOperatorCallCheckers.check(integerLiteralOperatorCall, data)
    }

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: CheckerContext) {
        checkers.allFunctionCallCheckers.check(implicitInvokeCall, data)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        checkers.allCallableReferenceAccessCheckers.check(callableReferenceAccess, data)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        checkers.allThisReceiverExpressionCheckers.check(thisReceiverExpression, data)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        checkers.allResolvedQualifierCheckers.check(resolvedQualifier, data)
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        checkers.allWhenExpressionCheckers.check(whenExpression, data)
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: CheckerContext) {
        checkers.allWhileLoopCheckers.check(whileLoop, data)
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: CheckerContext) {
        checkers.allDoWhileLoopCheckers.check(doWhileLoop, data)
    }

    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        checkers.allLoopExpressionCheckers.check(errorLoop, data)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        checkers.allLogicExpressionCheckers.check(binaryLogicExpression, data)
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        checkers.allArrayOfCallCheckers.check(arrayOfCall, data)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        checkers.allStringConcatenationCallCheckers.check(stringConcatenationCall, data)
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        checkers.allCheckNotNullCallCheckers.check(checkNotNullCall, data)
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: CheckerContext) {
        checkers.allElvisExpressionCheckers.check(elvisExpression, data)
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: CheckerContext) {
        checkers.allSafeCallExpressionCheckers.check(safeCallExpression, data)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        checkers.allTryExpressionCheckers.check(tryExpression, data)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        checkers.allClassReferenceExpressionCheckers.check(classReferenceExpression, data)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        checkers.allGetClassCallCheckers.check(getClassCall, data)
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: CheckerContext) {
        checkers.allEqualityOperatorCallCheckers.check(equalityOperatorCall, data)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
        checkers.allVariableAssignmentCheckers.check(variableAssignment, data)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: CheckerContext) {
        checkers.allReturnExpressionCheckers.check(returnExpression, data)
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: CheckerContext) {
        checkers.allLoopJumpCheckers.check(breakExpression, data)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: CheckerContext) {
        checkers.allLoopJumpCheckers.check(continueExpression, data)
    }

    override fun visitBlock(block: FirBlock, data: CheckerContext) {
        checkers.allBlockCheckers.check(block, data)
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: CheckerContext) {
        checkers.allCallCheckers.check(delegatedConstructorCall, data)
    }

    private fun <E : FirStatement> Collection<FirExpressionChecker<E>>.check(
        expression: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            checker.check(expression, context, reporter)
        }
    }
}
