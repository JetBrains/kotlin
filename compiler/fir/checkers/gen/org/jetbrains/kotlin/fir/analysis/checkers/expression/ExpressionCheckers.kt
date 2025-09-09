/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@Suppress("UNCHECKED_CAST")
abstract class ExpressionCheckers {
    companion object {
        val EMPTY: ExpressionCheckers = object : ExpressionCheckers() {}
    }

    open val basicExpressionCheckers: Set<FirBasicExpressionChecker> = emptySet()
    open val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = emptySet()
    open val callCheckers: Set<FirCallChecker> = emptySet()
    open val functionCallCheckers: Set<FirFunctionCallChecker> = emptySet()
    open val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = emptySet()
    open val superReceiverExpressionCheckers: Set<FirSuperReceiverExpressionChecker> = emptySet()
    open val integerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker> = emptySet()
    open val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = emptySet()
    open val tryExpressionCheckers: Set<FirTryExpressionChecker> = emptySet()
    open val whenExpressionCheckers: Set<FirWhenExpressionChecker> = emptySet()
    open val loopExpressionCheckers: Set<FirLoopExpressionChecker> = emptySet()
    open val loopJumpCheckers: Set<FirLoopJumpChecker> = emptySet()
    open val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker> = emptySet()
    open val returnExpressionCheckers: Set<FirReturnExpressionChecker> = emptySet()
    open val blockCheckers: Set<FirBlockChecker> = emptySet()
    open val replDeclarationReferenceCheckers: Set<FirReplDeclarationReferenceChecker> = emptySet()
    open val annotationCheckers: Set<FirAnnotationChecker> = emptySet()
    open val annotationCallCheckers: Set<FirAnnotationCallChecker> = emptySet()
    open val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = emptySet()
    open val elvisExpressionCheckers: Set<FirElvisExpressionChecker> = emptySet()
    open val getClassCallCheckers: Set<FirGetClassCallChecker> = emptySet()
    open val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = emptySet()
    open val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker> = emptySet()
    open val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = emptySet()
    open val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = emptySet()
    open val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = emptySet()
    open val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = emptySet()
    open val literalExpressionCheckers: Set<FirLiteralExpressionChecker> = emptySet()
    open val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = emptySet()
    open val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> = emptySet()
    open val whileLoopCheckers: Set<FirWhileLoopChecker> = emptySet()
    open val throwExpressionCheckers: Set<FirThrowExpressionChecker> = emptySet()
    open val doWhileLoopCheckers: Set<FirDoWhileLoopChecker> = emptySet()
    open val collectionLiteralCheckers: Set<FirCollectionLiteralChecker> = emptySet()
    open val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> = emptySet()
    open val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicExpressionCheckers: Array<FirBasicExpressionChecker> by lazy { basicExpressionCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allQualifiedAccessExpressionCheckers: Array<FirQualifiedAccessExpressionChecker> by lazy { (qualifiedAccessExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirQualifiedAccessExpressionChecker> }
    @CheckersComponentInternal internal val allCallCheckers: Array<FirCallChecker> by lazy { (callCheckers + basicExpressionCheckers).toTypedArray() as Array<FirCallChecker> }
    @CheckersComponentInternal internal val allFunctionCallCheckers: Array<FirFunctionCallChecker> by lazy { (functionCallCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirFunctionCallChecker> }
    @CheckersComponentInternal internal val allPropertyAccessExpressionCheckers: Array<FirPropertyAccessExpressionChecker> by lazy { (propertyAccessExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirPropertyAccessExpressionChecker> }
    @CheckersComponentInternal internal val allSuperReceiverExpressionCheckers: Array<FirSuperReceiverExpressionChecker> by lazy { (superReceiverExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirSuperReceiverExpressionChecker> }
    @CheckersComponentInternal internal val allIntegerLiteralOperatorCallCheckers: Array<FirIntegerLiteralOperatorCallChecker> by lazy { (integerLiteralOperatorCallCheckers + functionCallCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirIntegerLiteralOperatorCallChecker> }
    @CheckersComponentInternal internal val allVariableAssignmentCheckers: Array<FirVariableAssignmentChecker> by lazy { (variableAssignmentCheckers + basicExpressionCheckers).toTypedArray() as Array<FirVariableAssignmentChecker> }
    @CheckersComponentInternal internal val allTryExpressionCheckers: Array<FirTryExpressionChecker> by lazy { (tryExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirTryExpressionChecker> }
    @CheckersComponentInternal internal val allWhenExpressionCheckers: Array<FirWhenExpressionChecker> by lazy { (whenExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirWhenExpressionChecker> }
    @CheckersComponentInternal internal val allLoopExpressionCheckers: Array<FirLoopExpressionChecker> by lazy { (loopExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirLoopExpressionChecker> }
    @CheckersComponentInternal internal val allLoopJumpCheckers: Array<FirLoopJumpChecker> by lazy { (loopJumpCheckers + basicExpressionCheckers).toTypedArray() as Array<FirLoopJumpChecker> }
    @CheckersComponentInternal internal val allBooleanOperatorExpressionCheckers: Array<FirBooleanOperatorExpressionChecker> by lazy { (booleanOperatorExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirBooleanOperatorExpressionChecker> }
    @CheckersComponentInternal internal val allReturnExpressionCheckers: Array<FirReturnExpressionChecker> by lazy { (returnExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirReturnExpressionChecker> }
    @CheckersComponentInternal internal val allBlockCheckers: Array<FirBlockChecker> by lazy { (blockCheckers + basicExpressionCheckers).toTypedArray() as Array<FirBlockChecker> }
    @CheckersComponentInternal internal val allReplDeclarationReferenceCheckers: Array<FirReplDeclarationReferenceChecker> by lazy { (replDeclarationReferenceCheckers + basicExpressionCheckers).toTypedArray() as Array<FirReplDeclarationReferenceChecker> }
    @CheckersComponentInternal internal val allAnnotationCheckers: Array<FirAnnotationChecker> by lazy { (annotationCheckers + basicExpressionCheckers).toTypedArray() as Array<FirAnnotationChecker> }
    @CheckersComponentInternal internal val allAnnotationCallCheckers: Array<FirAnnotationCallChecker> by lazy { (annotationCallCheckers + annotationCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirAnnotationCallChecker> }
    @CheckersComponentInternal internal val allCheckNotNullCallCheckers: Array<FirCheckNotNullCallChecker> by lazy { (checkNotNullCallCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirCheckNotNullCallChecker> }
    @CheckersComponentInternal internal val allElvisExpressionCheckers: Array<FirElvisExpressionChecker> by lazy { (elvisExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirElvisExpressionChecker> }
    @CheckersComponentInternal internal val allGetClassCallCheckers: Array<FirGetClassCallChecker> by lazy { (getClassCallCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirGetClassCallChecker> }
    @CheckersComponentInternal internal val allSafeCallExpressionCheckers: Array<FirSafeCallExpressionChecker> by lazy { (safeCallExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirSafeCallExpressionChecker> }
    @CheckersComponentInternal internal val allSmartCastExpressionCheckers: Array<FirSmartCastExpressionChecker> by lazy { (smartCastExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirSmartCastExpressionChecker> }
    @CheckersComponentInternal internal val allEqualityOperatorCallCheckers: Array<FirEqualityOperatorCallChecker> by lazy { (equalityOperatorCallCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirEqualityOperatorCallChecker> }
    @CheckersComponentInternal internal val allStringConcatenationCallCheckers: Array<FirStringConcatenationCallChecker> by lazy { (stringConcatenationCallCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirStringConcatenationCallChecker> }
    @CheckersComponentInternal internal val allTypeOperatorCallCheckers: Array<FirTypeOperatorCallChecker> by lazy { (typeOperatorCallCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirTypeOperatorCallChecker> }
    @CheckersComponentInternal internal val allResolvedQualifierCheckers: Array<FirResolvedQualifierChecker> by lazy { (resolvedQualifierCheckers + basicExpressionCheckers).toTypedArray() as Array<FirResolvedQualifierChecker> }
    @CheckersComponentInternal internal val allLiteralExpressionCheckers: Array<FirLiteralExpressionChecker> by lazy { (literalExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirLiteralExpressionChecker> }
    @CheckersComponentInternal internal val allCallableReferenceAccessCheckers: Array<FirCallableReferenceAccessChecker> by lazy { (callableReferenceAccessCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirCallableReferenceAccessChecker> }
    @CheckersComponentInternal internal val allThisReceiverExpressionCheckers: Array<FirThisReceiverExpressionChecker> by lazy { (thisReceiverExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirThisReceiverExpressionChecker> }
    @CheckersComponentInternal internal val allWhileLoopCheckers: Array<FirWhileLoopChecker> by lazy { (whileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirWhileLoopChecker> }
    @CheckersComponentInternal internal val allThrowExpressionCheckers: Array<FirThrowExpressionChecker> by lazy { (throwExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirThrowExpressionChecker> }
    @CheckersComponentInternal internal val allDoWhileLoopCheckers: Array<FirDoWhileLoopChecker> by lazy { (doWhileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirDoWhileLoopChecker> }
    @CheckersComponentInternal internal val allCollectionLiteralCheckers: Array<FirCollectionLiteralChecker> by lazy { (collectionLiteralCheckers + basicExpressionCheckers + callCheckers).toTypedArray() as Array<FirCollectionLiteralChecker> }
    @CheckersComponentInternal internal val allClassReferenceExpressionCheckers: Array<FirClassReferenceExpressionChecker> by lazy { (classReferenceExpressionCheckers + basicExpressionCheckers).toTypedArray() as Array<FirClassReferenceExpressionChecker> }
    @CheckersComponentInternal internal val allInaccessibleReceiverCheckers: Array<FirInaccessibleReceiverChecker> by lazy { (inaccessibleReceiverCheckers + basicExpressionCheckers).toTypedArray() as Array<FirInaccessibleReceiverChecker> }
}
