/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class ExpressionCheckers {
    companion object {
        val EMPTY: ExpressionCheckers = object : ExpressionCheckers() {}
    }

    open val basicExpressionCheckers: Set<FirBasicExpressionChecker> = emptySet()
    open val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = emptySet()
    open val callCheckers: Set<FirCallChecker> = emptySet()
    open val functionCallCheckers: Set<FirFunctionCallChecker> = emptySet()
    open val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = emptySet()
    open val integerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker> = emptySet()
    open val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = emptySet()
    open val tryExpressionCheckers: Set<FirTryExpressionChecker> = emptySet()
    open val whenExpressionCheckers: Set<FirWhenExpressionChecker> = emptySet()
    open val loopExpressionCheckers: Set<FirLoopExpressionChecker> = emptySet()
    open val loopJumpCheckers: Set<FirLoopJumpChecker> = emptySet()
    open val logicExpressionCheckers: Set<FirLogicExpressionChecker> = emptySet()
    open val returnExpressionCheckers: Set<FirReturnExpressionChecker> = emptySet()
    open val blockCheckers: Set<FirBlockChecker> = emptySet()
    open val annotationCheckers: Set<FirAnnotationChecker> = emptySet()
    open val annotationCallCheckers: Set<FirAnnotationCallChecker> = emptySet()
    open val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = emptySet()
    open val elvisExpressionCheckers: Set<FirElvisExpressionChecker> = emptySet()
    open val getClassCallCheckers: Set<FirGetClassCallChecker> = emptySet()
    open val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = emptySet()
    open val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = emptySet()
    open val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = emptySet()
    open val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = emptySet()
    open val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = emptySet()
    open val constExpressionCheckers: Set<FirConstExpressionChecker> = emptySet()
    open val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = emptySet()
    open val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> = emptySet()
    open val whileLoopCheckers: Set<FirWhileLoopChecker> = emptySet()
    open val throwExpressionCheckers: Set<FirThrowExpressionChecker> = emptySet()
    open val doWhileLoopCheckers: Set<FirDoWhileLoopChecker> = emptySet()
    open val arrayOfCallCheckers: Set<FirArrayOfCallChecker> = emptySet()
    open val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> = emptySet()
    open val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicExpressionCheckers: Set<FirBasicExpressionChecker> by lazy { basicExpressionCheckers }
    @CheckersComponentInternal internal val allQualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> by lazy { qualifiedAccessExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allCallCheckers: Set<FirCallChecker> by lazy { callCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allFunctionCallCheckers: Set<FirFunctionCallChecker> by lazy { functionCallCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allPropertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> by lazy { propertyAccessExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allIntegerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker> by lazy { integerLiteralOperatorCallCheckers + functionCallCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allVariableAssignmentCheckers: Set<FirVariableAssignmentChecker> by lazy { variableAssignmentCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allTryExpressionCheckers: Set<FirTryExpressionChecker> by lazy { tryExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allWhenExpressionCheckers: Set<FirWhenExpressionChecker> by lazy { whenExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allLoopExpressionCheckers: Set<FirLoopExpressionChecker> by lazy { loopExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allLoopJumpCheckers: Set<FirLoopJumpChecker> by lazy { loopJumpCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allLogicExpressionCheckers: Set<FirLogicExpressionChecker> by lazy { logicExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allReturnExpressionCheckers: Set<FirReturnExpressionChecker> by lazy { returnExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allBlockCheckers: Set<FirBlockChecker> by lazy { blockCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allAnnotationCheckers: Set<FirAnnotationChecker> by lazy { annotationCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allAnnotationCallCheckers: Set<FirAnnotationCallChecker> by lazy { annotationCallCheckers + annotationCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allCheckNotNullCallCheckers: Set<FirCheckNotNullCallChecker> by lazy { checkNotNullCallCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allElvisExpressionCheckers: Set<FirElvisExpressionChecker> by lazy { elvisExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allGetClassCallCheckers: Set<FirGetClassCallChecker> by lazy { getClassCallCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allSafeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> by lazy { safeCallExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allEqualityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> by lazy { equalityOperatorCallCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allStringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> by lazy { stringConcatenationCallCheckers + callCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allTypeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> by lazy { typeOperatorCallCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allResolvedQualifierCheckers: Set<FirResolvedQualifierChecker> by lazy { resolvedQualifierCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allConstExpressionCheckers: Set<FirConstExpressionChecker> by lazy { constExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allCallableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> by lazy { callableReferenceAccessCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allThisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> by lazy { thisReceiverExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allWhileLoopCheckers: Set<FirWhileLoopChecker> by lazy { whileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allThrowExpressionCheckers: Set<FirThrowExpressionChecker> by lazy { throwExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allDoWhileLoopCheckers: Set<FirDoWhileLoopChecker> by lazy { doWhileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allArrayOfCallCheckers: Set<FirArrayOfCallChecker> by lazy { arrayOfCallCheckers + basicExpressionCheckers + callCheckers }
    @CheckersComponentInternal internal val allClassReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> by lazy { classReferenceExpressionCheckers + basicExpressionCheckers }
    @CheckersComponentInternal internal val allInaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> by lazy { inaccessibleReceiverCheckers + basicExpressionCheckers }
}
