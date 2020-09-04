/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

abstract class ExpressionCheckers {
    companion object {
        val EMPTY: ExpressionCheckers = object : ExpressionCheckers() {}
    }

    open val expressionCheckers: List<FirBasicExpressionChecker> = emptyList()
    open val qualifiedAccessCheckers: List<FirQualifiedAccessChecker> = emptyList()
    open val functionCallCheckers: List<FirFunctionCallChecker> = emptyList()
    open val variableAssignmentCheckers: List<FirVariableAssignmentChecker> = emptyList()

    internal val allExpressionCheckers get() = expressionCheckers
    internal val allQualifiedAccessCheckers get() = qualifiedAccessCheckers + allExpressionCheckers
    internal val allFunctionCallCheckers get() = functionCallCheckers + allQualifiedAccessCheckers
}
