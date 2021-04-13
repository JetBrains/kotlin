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

internal class ComposedExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = _basicExpressionCheckers
    override val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker>
        get() = _qualifiedAccessCheckers
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = _functionCallCheckers
    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = _variableAssignmentCheckers
    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        get() = _tryExpressionCheckers
    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = _whenExpressionCheckers
    override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
        get() = _returnExpressionCheckers
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = _annotationCallCheckers

    private val _basicExpressionCheckers: MutableSet<FirBasicExpressionChecker> = mutableSetOf()
    private val _qualifiedAccessCheckers: MutableSet<FirQualifiedAccessChecker> = mutableSetOf()
    private val _functionCallCheckers: MutableSet<FirFunctionCallChecker> = mutableSetOf()
    private val _variableAssignmentCheckers: MutableSet<FirVariableAssignmentChecker> = mutableSetOf()
    private val _tryExpressionCheckers: MutableSet<FirTryExpressionChecker> = mutableSetOf()
    private val _whenExpressionCheckers: MutableSet<FirWhenExpressionChecker> = mutableSetOf()
    private val _returnExpressionCheckers: MutableSet<FirReturnExpressionChecker> = mutableSetOf()
    private val _annotationCallCheckers: MutableSet<FirAnnotationCallChecker> = mutableSetOf()

    @CheckersComponentInternal
    internal fun register(checkers: ExpressionCheckers) {
        _basicExpressionCheckers += checkers.basicExpressionCheckers
        _qualifiedAccessCheckers += checkers.qualifiedAccessCheckers
        _functionCallCheckers += checkers.functionCallCheckers
        _variableAssignmentCheckers += checkers.variableAssignmentCheckers
        _tryExpressionCheckers += checkers.tryExpressionCheckers
        _whenExpressionCheckers += checkers.whenExpressionCheckers
        _returnExpressionCheckers += checkers.returnExpressionCheckers
        _annotationCallCheckers += checkers.annotationCallCheckers
    }
}
