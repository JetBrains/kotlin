/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsCodeConstantArgumentChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsQualifierChecker

object JsExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            FirJsQualifierChecker,
        )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            FirJsDefinedExternallyCallChecker,
            FirJsNativeRttiChecker,
            FirJsReflectionAPICallChecker
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirJsCodeConstantArgumentChecker,
            FirJsReifiedExternalChecker
        )

    override val callCheckers: Set<FirCallChecker>
        get() = setOf(
            FirJsExternalArgumentCallChecker
        )

    override val getClassCallCheckers: Set<FirGetClassCallChecker>
        get() = setOf(
            FirJsModuleGetClassCallChecker
        )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = setOf(
            FirJsModuleQualifiedAccessChecker,
            FirJsDynamicCallChecker
        )
}
