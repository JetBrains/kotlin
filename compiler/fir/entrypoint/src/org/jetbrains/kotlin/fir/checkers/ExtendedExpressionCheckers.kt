/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.*

object ExtendedExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = setOf(
        ArrayEqualityCanBeReplacedWithEquals,
        RedundantSingleExpressionStringTemplateChecker,
        EmptyRangeChecker,
    )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = setOf(
        CanBeReplacedWithOperatorAssignmentChecker,
    )

    override val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker> = setOf(
        RedundantCallOfConversionMethod,
        UselessCallOnNotNullChecker,
    )
}
