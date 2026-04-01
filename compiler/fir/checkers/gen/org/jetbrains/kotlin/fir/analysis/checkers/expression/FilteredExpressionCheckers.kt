/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FilteredExpressionCheckers(
    val delegate: ExpressionCheckers,
    val predicate: (FirExpressionChecker<*>) -> Boolean
) : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = delegate.basicExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = delegate.qualifiedAccessExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val callCheckers: Set<FirCallChecker> = delegate.callCheckers.filterTo(mutableSetOf(), predicate)
    override val functionCallCheckers: Set<FirFunctionCallChecker> = delegate.functionCallCheckers.filterTo(mutableSetOf(), predicate)
    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = delegate.propertyAccessExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val superReceiverExpressionCheckers: Set<FirSuperReceiverExpressionChecker> = delegate.superReceiverExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val integerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker> = delegate.integerLiteralOperatorCallCheckers.filterTo(mutableSetOf(), predicate)
    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = delegate.variableAssignmentCheckers.filterTo(mutableSetOf(), predicate)
    override val tryExpressionCheckers: Set<FirTryExpressionChecker> = delegate.tryExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val whenExpressionCheckers: Set<FirWhenExpressionChecker> = delegate.whenExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val loopExpressionCheckers: Set<FirLoopExpressionChecker> = delegate.loopExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val loopJumpCheckers: Set<FirLoopJumpChecker> = delegate.loopJumpCheckers.filterTo(mutableSetOf(), predicate)
    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker> = delegate.booleanOperatorExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = delegate.returnExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val blockCheckers: Set<FirBlockChecker> = delegate.blockCheckers.filterTo(mutableSetOf(), predicate)
    override val replDeclarationReferenceCheckers: Set<FirReplDeclarationReferenceChecker> = delegate.replDeclarationReferenceCheckers.filterTo(mutableSetOf(), predicate)
    override val replPropertyInitializerCheckers: Set<FirReplPropertyInitializerChecker> = delegate.replPropertyInitializerCheckers.filterTo(mutableSetOf(), predicate)
    override val replPropertyDelegateCheckers: Set<FirReplPropertyDelegateChecker> = delegate.replPropertyDelegateCheckers.filterTo(mutableSetOf(), predicate)
    override val replExpressionReferenceCheckers: Set<FirReplExpressionReferenceChecker> = delegate.replExpressionReferenceCheckers.filterTo(mutableSetOf(), predicate)
    override val annotationCheckers: Set<FirAnnotationChecker> = delegate.annotationCheckers.filterTo(mutableSetOf(), predicate)
    override val annotationCallCheckers: Set<FirAnnotationCallChecker> = delegate.annotationCallCheckers.filterTo(mutableSetOf(), predicate)
    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = delegate.checkNotNullCallCheckers.filterTo(mutableSetOf(), predicate)
    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker> = delegate.elvisExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val getClassCallCheckers: Set<FirGetClassCallChecker> = delegate.getClassCallCheckers.filterTo(mutableSetOf(), predicate)
    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = delegate.safeCallExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker> = delegate.smartCastExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = delegate.equalityOperatorCallCheckers.filterTo(mutableSetOf(), predicate)
    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = delegate.stringConcatenationCallCheckers.filterTo(mutableSetOf(), predicate)
    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = delegate.typeOperatorCallCheckers.filterTo(mutableSetOf(), predicate)
    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = delegate.resolvedQualifierCheckers.filterTo(mutableSetOf(), predicate)
    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker> = delegate.literalExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = delegate.callableReferenceAccessCheckers.filterTo(mutableSetOf(), predicate)
    override val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> = delegate.thisReceiverExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val whileLoopCheckers: Set<FirWhileLoopChecker> = delegate.whileLoopCheckers.filterTo(mutableSetOf(), predicate)
    override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = delegate.throwExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val doWhileLoopCheckers: Set<FirDoWhileLoopChecker> = delegate.doWhileLoopCheckers.filterTo(mutableSetOf(), predicate)
    override val collectionLiteralCheckers: Set<FirCollectionLiteralChecker> = delegate.collectionLiteralCheckers.filterTo(mutableSetOf(), predicate)
    override val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker> = delegate.classReferenceExpressionCheckers.filterTo(mutableSetOf(), predicate)
    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> = delegate.inaccessibleReceiverCheckers.filterTo(mutableSetOf(), predicate)
}
