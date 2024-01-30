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

class ComposedExpressionCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : ExpressionCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = _basicExpressionCheckers
    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = _qualifiedAccessExpressionCheckers
    override val callCheckers: Set<FirCallChecker>
        get() = _callCheckers
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = _functionCallCheckers
    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
        get() = _propertyAccessExpressionCheckers
    override val integerLiteralOperatorCallCheckers: Set<FirIntegerLiteralOperatorCallChecker>
        get() = _integerLiteralOperatorCallCheckers
    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = _variableAssignmentCheckers
    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        get() = _tryExpressionCheckers
    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = _whenExpressionCheckers
    override val loopExpressionCheckers: Set<FirLoopExpressionChecker>
        get() = _loopExpressionCheckers
    override val loopJumpCheckers: Set<FirLoopJumpChecker>
        get() = _loopJumpCheckers
    override val logicExpressionCheckers: Set<FirLogicExpressionChecker>
        get() = _logicExpressionCheckers
    override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
        get() = _returnExpressionCheckers
    override val blockCheckers: Set<FirBlockChecker>
        get() = _blockCheckers
    override val annotationCheckers: Set<FirAnnotationChecker>
        get() = _annotationCheckers
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = _annotationCallCheckers
    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker>
        get() = _checkNotNullCallCheckers
    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker>
        get() = _elvisExpressionCheckers
    override val getClassCallCheckers: Set<FirGetClassCallChecker>
        get() = _getClassCallCheckers
    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker>
        get() = _safeCallExpressionCheckers
    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker>
        get() = _equalityOperatorCallCheckers
    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker>
        get() = _stringConcatenationCallCheckers
    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker>
        get() = _typeOperatorCallCheckers
    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        get() = _resolvedQualifierCheckers
    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker>
        get() = _literalExpressionCheckers
    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker>
        get() = _callableReferenceAccessCheckers
    override val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker>
        get() = _thisReceiverExpressionCheckers
    override val whileLoopCheckers: Set<FirWhileLoopChecker>
        get() = _whileLoopCheckers
    override val throwExpressionCheckers: Set<FirThrowExpressionChecker>
        get() = _throwExpressionCheckers
    override val doWhileLoopCheckers: Set<FirDoWhileLoopChecker>
        get() = _doWhileLoopCheckers
    override val arrayLiteralCheckers: Set<FirArrayLiteralChecker>
        get() = _arrayLiteralCheckers
    override val classReferenceExpressionCheckers: Set<FirClassReferenceExpressionChecker>
        get() = _classReferenceExpressionCheckers
    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker>
        get() = _inaccessibleReceiverCheckers

    private val _basicExpressionCheckers: MutableSet<FirBasicExpressionChecker> = mutableSetOf()
    private val _qualifiedAccessExpressionCheckers: MutableSet<FirQualifiedAccessExpressionChecker> = mutableSetOf()
    private val _callCheckers: MutableSet<FirCallChecker> = mutableSetOf()
    private val _functionCallCheckers: MutableSet<FirFunctionCallChecker> = mutableSetOf()
    private val _propertyAccessExpressionCheckers: MutableSet<FirPropertyAccessExpressionChecker> = mutableSetOf()
    private val _integerLiteralOperatorCallCheckers: MutableSet<FirIntegerLiteralOperatorCallChecker> = mutableSetOf()
    private val _variableAssignmentCheckers: MutableSet<FirVariableAssignmentChecker> = mutableSetOf()
    private val _tryExpressionCheckers: MutableSet<FirTryExpressionChecker> = mutableSetOf()
    private val _whenExpressionCheckers: MutableSet<FirWhenExpressionChecker> = mutableSetOf()
    private val _loopExpressionCheckers: MutableSet<FirLoopExpressionChecker> = mutableSetOf()
    private val _loopJumpCheckers: MutableSet<FirLoopJumpChecker> = mutableSetOf()
    private val _logicExpressionCheckers: MutableSet<FirLogicExpressionChecker> = mutableSetOf()
    private val _returnExpressionCheckers: MutableSet<FirReturnExpressionChecker> = mutableSetOf()
    private val _blockCheckers: MutableSet<FirBlockChecker> = mutableSetOf()
    private val _annotationCheckers: MutableSet<FirAnnotationChecker> = mutableSetOf()
    private val _annotationCallCheckers: MutableSet<FirAnnotationCallChecker> = mutableSetOf()
    private val _checkNotNullCallCheckers: MutableSet<FirCheckNotNullCallChecker> = mutableSetOf()
    private val _elvisExpressionCheckers: MutableSet<FirElvisExpressionChecker> = mutableSetOf()
    private val _getClassCallCheckers: MutableSet<FirGetClassCallChecker> = mutableSetOf()
    private val _safeCallExpressionCheckers: MutableSet<FirSafeCallExpressionChecker> = mutableSetOf()
    private val _equalityOperatorCallCheckers: MutableSet<FirEqualityOperatorCallChecker> = mutableSetOf()
    private val _stringConcatenationCallCheckers: MutableSet<FirStringConcatenationCallChecker> = mutableSetOf()
    private val _typeOperatorCallCheckers: MutableSet<FirTypeOperatorCallChecker> = mutableSetOf()
    private val _resolvedQualifierCheckers: MutableSet<FirResolvedQualifierChecker> = mutableSetOf()
    private val _literalExpressionCheckers: MutableSet<FirLiteralExpressionChecker> = mutableSetOf()
    private val _callableReferenceAccessCheckers: MutableSet<FirCallableReferenceAccessChecker> = mutableSetOf()
    private val _thisReceiverExpressionCheckers: MutableSet<FirThisReceiverExpressionChecker> = mutableSetOf()
    private val _whileLoopCheckers: MutableSet<FirWhileLoopChecker> = mutableSetOf()
    private val _throwExpressionCheckers: MutableSet<FirThrowExpressionChecker> = mutableSetOf()
    private val _doWhileLoopCheckers: MutableSet<FirDoWhileLoopChecker> = mutableSetOf()
    private val _arrayLiteralCheckers: MutableSet<FirArrayLiteralChecker> = mutableSetOf()
    private val _classReferenceExpressionCheckers: MutableSet<FirClassReferenceExpressionChecker> = mutableSetOf()
    private val _inaccessibleReceiverCheckers: MutableSet<FirInaccessibleReceiverChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: ExpressionCheckers) {
        checkers.basicExpressionCheckers.filterTo(_basicExpressionCheckers, predicate)
        checkers.qualifiedAccessExpressionCheckers.filterTo(_qualifiedAccessExpressionCheckers, predicate)
        checkers.callCheckers.filterTo(_callCheckers, predicate)
        checkers.functionCallCheckers.filterTo(_functionCallCheckers, predicate)
        checkers.propertyAccessExpressionCheckers.filterTo(_propertyAccessExpressionCheckers, predicate)
        checkers.integerLiteralOperatorCallCheckers.filterTo(_integerLiteralOperatorCallCheckers, predicate)
        checkers.variableAssignmentCheckers.filterTo(_variableAssignmentCheckers, predicate)
        checkers.tryExpressionCheckers.filterTo(_tryExpressionCheckers, predicate)
        checkers.whenExpressionCheckers.filterTo(_whenExpressionCheckers, predicate)
        checkers.loopExpressionCheckers.filterTo(_loopExpressionCheckers, predicate)
        checkers.loopJumpCheckers.filterTo(_loopJumpCheckers, predicate)
        checkers.logicExpressionCheckers.filterTo(_logicExpressionCheckers, predicate)
        checkers.returnExpressionCheckers.filterTo(_returnExpressionCheckers, predicate)
        checkers.blockCheckers.filterTo(_blockCheckers, predicate)
        checkers.annotationCheckers.filterTo(_annotationCheckers, predicate)
        checkers.annotationCallCheckers.filterTo(_annotationCallCheckers, predicate)
        checkers.checkNotNullCallCheckers.filterTo(_checkNotNullCallCheckers, predicate)
        checkers.elvisExpressionCheckers.filterTo(_elvisExpressionCheckers, predicate)
        checkers.getClassCallCheckers.filterTo(_getClassCallCheckers, predicate)
        checkers.safeCallExpressionCheckers.filterTo(_safeCallExpressionCheckers, predicate)
        checkers.equalityOperatorCallCheckers.filterTo(_equalityOperatorCallCheckers, predicate)
        checkers.stringConcatenationCallCheckers.filterTo(_stringConcatenationCallCheckers, predicate)
        checkers.typeOperatorCallCheckers.filterTo(_typeOperatorCallCheckers, predicate)
        checkers.resolvedQualifierCheckers.filterTo(_resolvedQualifierCheckers, predicate)
        checkers.literalExpressionCheckers.filterTo(_literalExpressionCheckers, predicate)
        checkers.callableReferenceAccessCheckers.filterTo(_callableReferenceAccessCheckers, predicate)
        checkers.thisReceiverExpressionCheckers.filterTo(_thisReceiverExpressionCheckers, predicate)
        checkers.whileLoopCheckers.filterTo(_whileLoopCheckers, predicate)
        checkers.throwExpressionCheckers.filterTo(_throwExpressionCheckers, predicate)
        checkers.doWhileLoopCheckers.filterTo(_doWhileLoopCheckers, predicate)
        checkers.arrayLiteralCheckers.filterTo(_arrayLiteralCheckers, predicate)
        checkers.classReferenceExpressionCheckers.filterTo(_classReferenceExpressionCheckers, predicate)
        checkers.inaccessibleReceiverCheckers.filterTo(_inaccessibleReceiverCheckers, predicate)
    }
}
