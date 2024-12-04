/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.experimental.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object ExperimentalExpressionCheckers : ExpressionCheckers() {
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            EmptyRangeChecker,
        )

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker>
        get() = setOf(
            RedundantInterpolationPrefixCheckerConcatenation,
        )

    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker>
        get() = setOf(
            RedundantInterpolationPrefixCheckerLiteral,
        )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = setOf(
            CanBeReplacedWithOperatorAssignmentChecker,
        )
}
