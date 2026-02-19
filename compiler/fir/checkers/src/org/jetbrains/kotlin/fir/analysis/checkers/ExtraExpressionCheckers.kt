/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.extra.*

object ExtraExpressionCheckers : ExpressionCheckers() {
    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = setOf(
        UselessCallOnNotNullChecker,
    )

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = setOf(
        RedundantSingleExpressionStringTemplateChecker,
    )
}
