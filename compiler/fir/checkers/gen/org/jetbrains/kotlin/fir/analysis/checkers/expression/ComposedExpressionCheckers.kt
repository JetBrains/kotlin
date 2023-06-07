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

class ComposedExpressionCheckers : ExpressionCheckers() {
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
    override val constExpressionCheckers: Set<FirConstExpressionChecker>
        get() = _constExpressionCheckers
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
    override val arrayOfCallCheckers: Set<FirArrayOfCallChecker>
        get() = _arrayOfCallCheckers
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
    private val _constExpressionCheckers: MutableSet<FirConstExpressionChecker> = mutableSetOf()
    private val _callableReferenceAccessCheckers: MutableSet<FirCallableReferenceAccessChecker> = mutableSetOf()
    private val _thisReceiverExpressionCheckers: MutableSet<FirThisReceiverExpressionChecker> = mutableSetOf()
    private val _whileLoopCheckers: MutableSet<FirWhileLoopChecker> = mutableSetOf()
    private val _throwExpressionCheckers: MutableSet<FirThrowExpressionChecker> = mutableSetOf()
    private val _doWhileLoopCheckers: MutableSet<FirDoWhileLoopChecker> = mutableSetOf()
    private val _arrayOfCallCheckers: MutableSet<FirArrayOfCallChecker> = mutableSetOf()
    private val _classReferenceExpressionCheckers: MutableSet<FirClassReferenceExpressionChecker> = mutableSetOf()
    private val _inaccessibleReceiverCheckers: MutableSet<FirInaccessibleReceiverChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: ExpressionCheckers) {
        _basicExpressionCheckers += checkers.basicExpressionCheckers
        _qualifiedAccessExpressionCheckers += checkers.qualifiedAccessExpressionCheckers
        _callCheckers += checkers.callCheckers
        _functionCallCheckers += checkers.functionCallCheckers
        _propertyAccessExpressionCheckers += checkers.propertyAccessExpressionCheckers
        _integerLiteralOperatorCallCheckers += checkers.integerLiteralOperatorCallCheckers
        _variableAssignmentCheckers += checkers.variableAssignmentCheckers
        _tryExpressionCheckers += checkers.tryExpressionCheckers
        _whenExpressionCheckers += checkers.whenExpressionCheckers
        _loopExpressionCheckers += checkers.loopExpressionCheckers
        _loopJumpCheckers += checkers.loopJumpCheckers
        _logicExpressionCheckers += checkers.logicExpressionCheckers
        _returnExpressionCheckers += checkers.returnExpressionCheckers
        _blockCheckers += checkers.blockCheckers
        _annotationCheckers += checkers.annotationCheckers
        _annotationCallCheckers += checkers.annotationCallCheckers
        _checkNotNullCallCheckers += checkers.checkNotNullCallCheckers
        _elvisExpressionCheckers += checkers.elvisExpressionCheckers
        _getClassCallCheckers += checkers.getClassCallCheckers
        _safeCallExpressionCheckers += checkers.safeCallExpressionCheckers
        _equalityOperatorCallCheckers += checkers.equalityOperatorCallCheckers
        _stringConcatenationCallCheckers += checkers.stringConcatenationCallCheckers
        _typeOperatorCallCheckers += checkers.typeOperatorCallCheckers
        _resolvedQualifierCheckers += checkers.resolvedQualifierCheckers
        _constExpressionCheckers += checkers.constExpressionCheckers
        _callableReferenceAccessCheckers += checkers.callableReferenceAccessCheckers
        _thisReceiverExpressionCheckers += checkers.thisReceiverExpressionCheckers
        _whileLoopCheckers += checkers.whileLoopCheckers
        _throwExpressionCheckers += checkers.throwExpressionCheckers
        _doWhileLoopCheckers += checkers.doWhileLoopCheckers
        _arrayOfCallCheckers += checkers.arrayOfCallCheckers
        _classReferenceExpressionCheckers += checkers.classReferenceExpressionCheckers
        _inaccessibleReceiverCheckers += checkers.inaccessibleReceiverCheckers
    }
}
