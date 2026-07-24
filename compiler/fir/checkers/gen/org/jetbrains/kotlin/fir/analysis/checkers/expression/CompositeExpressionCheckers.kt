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

class CompositeExpressionCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : ExpressionCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        field: MutableSet<FirBasicExpressionChecker> = []
    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        field: MutableSet<FirQualifiedAccessExpressionChecker> = []
    override val callCheckers: Set<FirCallChecker>
        field: MutableSet<FirCallChecker> = []
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        field: MutableSet<FirFunctionCallChecker> = []
    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
        field: MutableSet<FirPropertyAccessExpressionChecker> = []
    override val superReceiverExpressionCheckers: Set<FirSuperReceiverExpressionChecker>
        field: MutableSet<FirSuperReceiverExpressionChecker> = []
    override val integerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker>
        field: MutableSet<FirIntegerLiteralOperatorCallChecker> = []
    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        field: MutableSet<FirVariableAssignmentChecker> = []
    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        field: MutableSet<FirTryExpressionChecker> = []
    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        field: MutableSet<FirWhenExpressionChecker> = []
    override val loopExpressionCheckers: Set<FirLoopExpressionChecker>
        field: MutableSet<FirLoopExpressionChecker> = []
    override val loopJumpCheckers: Set<FirLoopJumpChecker>
        field: MutableSet<FirLoopJumpChecker> = []
    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker>
        field: MutableSet<FirBooleanOperatorExpressionChecker> = []
    override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
        field: MutableSet<FirReturnExpressionChecker> = []
    override val blockCheckers: Set<FirBlockChecker>
        field: MutableSet<FirBlockChecker> = []
    override val replDeclarationReferenceCheckers: Set<FirReplDeclarationReferenceChecker>
        field: MutableSet<FirReplDeclarationReferenceChecker> = []
    override val replPropertyInitializerCheckers: Set<FirReplPropertyInitializerChecker>
        field: MutableSet<FirReplPropertyInitializerChecker> = []
    override val replPropertyDelegateCheckers: Set<FirReplPropertyDelegateChecker>
        field: MutableSet<FirReplPropertyDelegateChecker> = []
    override val replExpressionReferenceCheckers: Set<FirReplExpressionReferenceChecker>
        field: MutableSet<FirReplExpressionReferenceChecker> = []
    override val annotationCheckers: Set<FirAnnotationChecker>
        field: MutableSet<FirAnnotationChecker> = []
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        field: MutableSet<FirAnnotationCallChecker> = []
    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker>
        field: MutableSet<FirCheckNotNullCallChecker> = []
    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker>
        field: MutableSet<FirElvisExpressionChecker> = []
    override val getClassCallCheckers: Set<FirGetClassCallChecker>
        field: MutableSet<FirGetClassCallChecker> = []
    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker>
        field: MutableSet<FirSafeCallExpressionChecker> = []
    override val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker>
        field: MutableSet<FirSmartCastExpressionChecker> = []
    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker>
        field: MutableSet<FirEqualityOperatorCallChecker> = []
    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker>
        field: MutableSet<FirStringConcatenationCallChecker> = []
    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker>
        field: MutableSet<FirTypeOperatorCallChecker> = []
    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        field: MutableSet<FirResolvedQualifierChecker> = []
    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker>
        field: MutableSet<FirLiteralExpressionChecker> = []
    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker>
        field: MutableSet<FirCallableReferenceAccessChecker> = []
    override val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker>
        field: MutableSet<FirThisReceiverExpressionChecker> = []
    override val whileLoopCheckers: Set<FirWhileLoopChecker>
        field: MutableSet<FirWhileLoopChecker> = []
    override val throwExpressionCheckers: Set<FirThrowExpressionChecker>
        field: MutableSet<FirThrowExpressionChecker> = []
    override val doWhileLoopCheckers: Set<FirDoWhileLoopChecker>
        field: MutableSet<FirDoWhileLoopChecker> = []
    override val collectionLiteralCheckers: Set<FirCollectionLiteralChecker>
        field: MutableSet<FirCollectionLiteralChecker> = []
    override val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker>
        field: MutableSet<FirClassReferenceExpressionChecker> = []
    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker>
        field: MutableSet<FirInaccessibleReceiverChecker> = []

    @CheckersComponentInternal
    fun register(checkers: ExpressionCheckers) {
        checkers.basicExpressionCheckers.filterTo(basicExpressionCheckers, predicate)
        checkers.qualifiedAccessExpressionCheckers.filterTo(qualifiedAccessExpressionCheckers, predicate)
        checkers.callCheckers.filterTo(callCheckers, predicate)
        checkers.functionCallCheckers.filterTo(functionCallCheckers, predicate)
        checkers.propertyAccessExpressionCheckers.filterTo(propertyAccessExpressionCheckers, predicate)
        checkers.superReceiverExpressionCheckers.filterTo(superReceiverExpressionCheckers, predicate)
        checkers.integerLiteralOperatorCallCheckers.filterTo(integerLiteralOperatorCallCheckers, predicate)
        checkers.variableAssignmentCheckers.filterTo(variableAssignmentCheckers, predicate)
        checkers.tryExpressionCheckers.filterTo(tryExpressionCheckers, predicate)
        checkers.whenExpressionCheckers.filterTo(whenExpressionCheckers, predicate)
        checkers.loopExpressionCheckers.filterTo(loopExpressionCheckers, predicate)
        checkers.loopJumpCheckers.filterTo(loopJumpCheckers, predicate)
        checkers.booleanOperatorExpressionCheckers.filterTo(booleanOperatorExpressionCheckers, predicate)
        checkers.returnExpressionCheckers.filterTo(returnExpressionCheckers, predicate)
        checkers.blockCheckers.filterTo(blockCheckers, predicate)
        checkers.replDeclarationReferenceCheckers.filterTo(replDeclarationReferenceCheckers, predicate)
        checkers.replPropertyInitializerCheckers.filterTo(replPropertyInitializerCheckers, predicate)
        checkers.replPropertyDelegateCheckers.filterTo(replPropertyDelegateCheckers, predicate)
        checkers.replExpressionReferenceCheckers.filterTo(replExpressionReferenceCheckers, predicate)
        checkers.annotationCheckers.filterTo(annotationCheckers, predicate)
        checkers.annotationCallCheckers.filterTo(annotationCallCheckers, predicate)
        checkers.checkNotNullCallCheckers.filterTo(checkNotNullCallCheckers, predicate)
        checkers.elvisExpressionCheckers.filterTo(elvisExpressionCheckers, predicate)
        checkers.getClassCallCheckers.filterTo(getClassCallCheckers, predicate)
        checkers.safeCallExpressionCheckers.filterTo(safeCallExpressionCheckers, predicate)
        checkers.smartCastExpressionCheckers.filterTo(smartCastExpressionCheckers, predicate)
        checkers.equalityOperatorCallCheckers.filterTo(equalityOperatorCallCheckers, predicate)
        checkers.stringConcatenationCallCheckers.filterTo(stringConcatenationCallCheckers, predicate)
        checkers.typeOperatorCallCheckers.filterTo(typeOperatorCallCheckers, predicate)
        checkers.resolvedQualifierCheckers.filterTo(resolvedQualifierCheckers, predicate)
        checkers.literalExpressionCheckers.filterTo(literalExpressionCheckers, predicate)
        checkers.callableReferenceAccessCheckers.filterTo(callableReferenceAccessCheckers, predicate)
        checkers.thisReceiverExpressionCheckers.filterTo(thisReceiverExpressionCheckers, predicate)
        checkers.whileLoopCheckers.filterTo(whileLoopCheckers, predicate)
        checkers.throwExpressionCheckers.filterTo(throwExpressionCheckers, predicate)
        checkers.doWhileLoopCheckers.filterTo(doWhileLoopCheckers, predicate)
        checkers.collectionLiteralCheckers.filterTo(collectionLiteralCheckers, predicate)
        checkers.classReferenceExpressionCheckers.filterTo(classReferenceExpressionCheckers, predicate)
        checkers.inaccessibleReceiverCheckers.filterTo(inaccessibleReceiverCheckers, predicate)
    }
}
