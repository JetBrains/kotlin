/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpresionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.*

object ExtendedExpressionCheckers : ExpressionCheckers() {
    override val expressionCheckers: List<FirBasicExpresionChecker> = listOf(
        ArrayEqualityCanBeReplacedWithEquals,
        RedundantSingleExpressionStringTemplateChecker,
        EmptyRangeChecker
    )

    override val variableAssignmentCheckers: List<FirVariableAssignmentChecker> = listOf(
        CanBeReplacedWithOperatorAssignmentChecker
    )

    override val qualifiedAccessCheckers: List<FirQualifiedAccessChecker> = listOf(
        RedundantCallOfConversionMethod
    )
}
