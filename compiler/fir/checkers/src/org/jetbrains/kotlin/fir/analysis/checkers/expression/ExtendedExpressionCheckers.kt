/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.extended.ArrayEqualityCanBeReplacedWithEquals
import org.jetbrains.kotlin.fir.analysis.checkers.extended.CanBeReplacedWithOperatorAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.EmptyRangeChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantSingleExpressionStringTemplateChecker

object ExtendedExpressionCheckers : ExpressionCheckers() {
    override val expressionCheckers: List<FirBasicExpresionChecker> = listOf(
        ArrayEqualityCanBeReplacedWithEquals,
        RedundantSingleExpressionStringTemplateChecker,
        EmptyRangeChecker
    )
    override val variableAssignmentCheckers: List<FirVariableAssignmentChecker> = listOf(
        CanBeReplacedWithOperatorAssignmentChecker
    )
}