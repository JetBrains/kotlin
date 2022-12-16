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
    checkers: ExpressionCheckers = session.checkersComponent.expressionCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private val allTypeOperatorCallCheckers = checkers.allTypeOperatorCallCheckers.toList()
    private val allConstExpressionCheckers = checkers.allConstExpressionCheckers.toList()
    private val allAnnotationCheckers = checkers.allAnnotationCheckers.toList()
    private val allAnnotationCallCheckers = checkers.allAnnotationCallCheckers.toList()
    private val allQualifiedAccessExpressionCheckers = checkers.allQualifiedAccessExpressionCheckers.toList()
    private val allPropertyAccessExpressionCheckers = checkers.allPropertyAccessExpressionCheckers.toList()
    private val allFunctionCallCheckers = checkers.allFunctionCallCheckers.toList()
    private val allIntegerLiteralOperatorCallCheckers = checkers.allIntegerLiteralOperatorCallCheckers.toList()
    private val allCallableReferenceAccessCheckers = checkers.allCallableReferenceAccessCheckers.toList()
    private val allThisReceiverExpressionCheckers = checkers.allThisReceiverExpressionCheckers.toList()
    private val allResolvedQualifierCheckers = checkers.allResolvedQualifierCheckers.toList()
    private val allWhenExpressionCheckers = checkers.allWhenExpressionCheckers.toList()
    private val allWhileLoopCheckers = checkers.allWhileLoopCheckers.toList()
    private val allDoWhileLoopCheckers = checkers.allDoWhileLoopCheckers.toList()
    private val allLoopExpressionCheckers = checkers.allLoopExpressionCheckers.toList()
    private val allLogicExpressionCheckers = checkers.allLogicExpressionCheckers.toList()
    private val allArrayOfCallCheckers = checkers.allArrayOfCallCheckers.toList()
    private val allStringConcatenationCallCheckers = checkers.allStringConcatenationCallCheckers.toList()
    private val allCheckNotNullCallCheckers = checkers.allCheckNotNullCallCheckers.toList()
    private val allElvisExpressionCheckers = checkers.allElvisExpressionCheckers.toList()
    private val allSafeCallExpressionCheckers = checkers.allSafeCallExpressionCheckers.toList()
    private val allTryExpressionCheckers = checkers.allTryExpressionCheckers.toList()
    private val allClassReferenceExpressionCheckers = checkers.allClassReferenceExpressionCheckers.toList()
    private val allGetClassCallCheckers = checkers.allGetClassCallCheckers.toList()
    private val allEqualityOperatorCallCheckers = checkers.allEqualityOperatorCallCheckers.toList()
    private val allVariableAssignmentCheckers = checkers.allVariableAssignmentCheckers.toList()
    private val allReturnExpressionCheckers = checkers.allReturnExpressionCheckers.toList()
    private val allLoopJumpCheckers = checkers.allLoopJumpCheckers.toList()
    private val allBlockCheckers = checkers.allBlockCheckers.toList()
    private val allCallCheckers = checkers.allCallCheckers.toList()

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: CheckerContext) {
        allTypeOperatorCallCheckers.check(typeOperatorCall, data)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: CheckerContext) {
        allConstExpressionCheckers.check(constExpression, data)
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: CheckerContext) {
        allAnnotationCheckers.check(annotation, data)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: CheckerContext) {
        allAnnotationCallCheckers.check(annotationCall, data)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
        allQualifiedAccessExpressionCheckers.check(qualifiedAccessExpression, data)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: CheckerContext) {
        allPropertyAccessExpressionCheckers.check(propertyAccessExpression, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
        allFunctionCallCheckers.check(functionCall, data)
    }

    override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: CheckerContext) {
        allIntegerLiteralOperatorCallCheckers.check(integerLiteralOperatorCall, data)
    }

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: CheckerContext) {
        allFunctionCallCheckers.check(implicitInvokeCall, data)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: CheckerContext) {
        allCallableReferenceAccessCheckers.check(callableReferenceAccess, data)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: CheckerContext) {
        allThisReceiverExpressionCheckers.check(thisReceiverExpression, data)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
        allResolvedQualifierCheckers.check(resolvedQualifier, data)
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: CheckerContext) {
        allWhenExpressionCheckers.check(whenExpression, data)
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: CheckerContext) {
        allWhileLoopCheckers.check(whileLoop, data)
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: CheckerContext) {
        allDoWhileLoopCheckers.check(doWhileLoop, data)
    }

    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        allLoopExpressionCheckers.check(errorLoop, data)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: CheckerContext) {
        allLogicExpressionCheckers.check(binaryLogicExpression, data)
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: CheckerContext) {
        allArrayOfCallCheckers.check(arrayOfCall, data)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: CheckerContext) {
        allStringConcatenationCallCheckers.check(stringConcatenationCall, data)
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: CheckerContext) {
        allCheckNotNullCallCheckers.check(checkNotNullCall, data)
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: CheckerContext) {
        allElvisExpressionCheckers.check(elvisExpression, data)
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: CheckerContext) {
        allSafeCallExpressionCheckers.check(safeCallExpression, data)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: CheckerContext) {
        allTryExpressionCheckers.check(tryExpression, data)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: CheckerContext) {
        allClassReferenceExpressionCheckers.check(classReferenceExpression, data)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: CheckerContext) {
        allGetClassCallCheckers.check(getClassCall, data)
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: CheckerContext) {
        allEqualityOperatorCallCheckers.check(equalityOperatorCall, data)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
        allVariableAssignmentCheckers.check(variableAssignment, data)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: CheckerContext) {
        allReturnExpressionCheckers.check(returnExpression, data)
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: CheckerContext) {
        allLoopJumpCheckers.check(breakExpression, data)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: CheckerContext) {
        allLoopJumpCheckers.check(continueExpression, data)
    }

    override fun visitBlock(block: FirBlock, data: CheckerContext) {
        allBlockCheckers.check(block, data)
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: CheckerContext) {
        allCallCheckers.check(delegatedConstructorCall, data)
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
