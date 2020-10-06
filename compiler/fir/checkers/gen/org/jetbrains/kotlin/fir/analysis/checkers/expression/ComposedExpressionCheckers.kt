/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    private val _basicExpressionCheckers: MutableSet<FirBasicExpressionChecker> = mutableSetOf()
    private val _qualifiedAccessCheckers: MutableSet<FirQualifiedAccessChecker> = mutableSetOf()
    private val _functionCallCheckers: MutableSet<FirFunctionCallChecker> = mutableSetOf()
    private val _variableAssignmentCheckers: MutableSet<FirVariableAssignmentChecker> = mutableSetOf()

    @CheckersComponentInternal
    internal fun register(checkers: ExpressionCheckers) {
        _basicExpressionCheckers += checkers.allBasicExpressionCheckers
        _qualifiedAccessCheckers += checkers.allQualifiedAccessCheckers
        _functionCallCheckers += checkers.allFunctionCallCheckers
        _variableAssignmentCheckers += checkers.allVariableAssignmentCheckers
    }
}
