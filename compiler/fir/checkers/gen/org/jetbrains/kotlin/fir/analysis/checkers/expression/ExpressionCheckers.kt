/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    open val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker> = emptySet()
    open val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = emptySet()
    open val functionCallCheckers: Set<FirFunctionCallChecker> = emptySet()
    open val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = emptySet()
    open val tryExpressionCheckers: Set<FirTryExpressionChecker> = emptySet()
    open val whenExpressionCheckers: Set<FirWhenExpressionChecker> = emptySet()
    open val loopExpressionCheckers: Set<FirLoopExpressionChecker> = emptySet()
    open val logicExpressionCheckers: Set<FirLogicExpressionChecker> = emptySet()
    open val returnExpressionCheckers: Set<FirReturnExpressionChecker> = emptySet()
    open val blockCheckers: Set<FirBlockChecker> = emptySet()
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
    open val doWhileLoopCheckers: Set<FirDoWhileLoopChecker> = emptySet()
    open val arrayOfCallCheckers: Set<FirArrayOfCallChecker> = emptySet()
    open val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicExpressionCheckers: Set<FirBasicExpressionChecker> get() = basicExpressionCheckers
    @CheckersComponentInternal internal val allQualifiedAccessCheckers: Set<FirQualifiedAccessChecker> get() = qualifiedAccessCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allQualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> get() = qualifiedAccessExpressionCheckers + basicExpressionCheckers + qualifiedAccessCheckers
    @CheckersComponentInternal internal val allFunctionCallCheckers: Set<FirFunctionCallChecker> get() = functionCallCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + qualifiedAccessCheckers
    @CheckersComponentInternal internal val allVariableAssignmentCheckers: Set<FirVariableAssignmentChecker> get() = variableAssignmentCheckers + qualifiedAccessCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allTryExpressionCheckers: Set<FirTryExpressionChecker> get() = tryExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allWhenExpressionCheckers: Set<FirWhenExpressionChecker> get() = whenExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allLoopExpressionCheckers: Set<FirLoopExpressionChecker> get() = loopExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allLogicExpressionCheckers: Set<FirLogicExpressionChecker> get() = logicExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allReturnExpressionCheckers: Set<FirReturnExpressionChecker> get() = returnExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allBlockCheckers: Set<FirBlockChecker> get() = blockCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allAnnotationCallCheckers: Set<FirAnnotationCallChecker> get() = annotationCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allCheckNotNullCallCheckers: Set<FirCheckNotNullCallChecker> get() = checkNotNullCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allElvisExpressionCheckers: Set<FirElvisExpressionChecker> get() = elvisExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allGetClassCallCheckers: Set<FirGetClassCallChecker> get() = getClassCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allSafeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> get() = safeCallExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allEqualityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> get() = equalityOperatorCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allStringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> get() = stringConcatenationCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allTypeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> get() = typeOperatorCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allResolvedQualifierCheckers: Set<FirResolvedQualifierChecker> get() = resolvedQualifierCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allConstExpressionCheckers: Set<FirConstExpressionChecker> get() = constExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allCallableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> get() = callableReferenceAccessCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + qualifiedAccessCheckers
    @CheckersComponentInternal internal val allThisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> get() = thisReceiverExpressionCheckers + qualifiedAccessExpressionCheckers + basicExpressionCheckers + qualifiedAccessCheckers
    @CheckersComponentInternal internal val allWhileLoopCheckers: Set<FirWhileLoopChecker> get() = whileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allDoWhileLoopCheckers: Set<FirDoWhileLoopChecker> get() = doWhileLoopCheckers + loopExpressionCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allArrayOfCallCheckers: Set<FirArrayOfCallChecker> get() = arrayOfCallCheckers + basicExpressionCheckers
    @CheckersComponentInternal internal val allClassReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> get() = classReferenceExpressionCheckers + basicExpressionCheckers
}
