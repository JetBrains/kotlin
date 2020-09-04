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

abstract class ExpressionCheckers {
    companion object {
        val EMPTY: ExpressionCheckers = object : ExpressionCheckers() {}
    }

    open val basicExpressionCheckers: Set<FirBasicExpressionChecker> = emptySet()
    open val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker> = emptySet()
    open val functionCallCheckers: Set<FirFunctionCallChecker> = emptySet()
    open val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicExpressionCheckers: Set<FirBasicExpressionChecker> get() = basicExpressionCheckers
    @CheckersComponentInternal internal val allQualifiedAccessCheckers: Set<FirQualifiedAccessChecker> get() = qualifiedAccessCheckers + allBasicExpressionCheckers
    @CheckersComponentInternal internal val allFunctionCallCheckers: Set<FirFunctionCallChecker> get() = functionCallCheckers + allQualifiedAccessCheckers
    @CheckersComponentInternal internal val allVariableAssignmentCheckers: Set<FirVariableAssignmentChecker> get() = variableAssignmentCheckers + allBasicExpressionCheckers
}
