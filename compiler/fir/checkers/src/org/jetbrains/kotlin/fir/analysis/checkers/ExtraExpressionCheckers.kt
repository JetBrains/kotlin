/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.extra.*

object ExtraExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            ArrayEqualityCanBeReplacedWithEquals,
        )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = setOf(
            RedundantCallOfConversionMethod,
            UselessCallOnNotNullChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            PlatformClassMappedToKotlinConstructorCallChecker,
        )

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker>
        get() = setOf(
            RedundantSingleExpressionStringTemplateChecker,
        )
}
