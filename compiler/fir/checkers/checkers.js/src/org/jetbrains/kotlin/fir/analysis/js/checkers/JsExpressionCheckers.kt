/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsCodeConstantArgumentChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsQualifierChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirWebReflectionAPICallChecker

object JsExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker> = setOf(
        FirJsQualifierChecker,
    )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = setOf(
        FirJsDefinedExternallyCallChecker,
        FirJsNativeRttiChecker,
        FirWebReflectionAPICallChecker(isWasm = false),
    )

    override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
        FirJsCodeConstantArgumentChecker,
        FirJsReifiedExternalChecker,
        FirJsReifiedJsNoRuntimeChecker,
    )

    override val callCheckers: Set<FirCallChecker> = setOf(
        FirJsExternalArgumentCallChecker
    )

    override val getClassCallCheckers: Set<FirGetClassCallChecker> = setOf(
        FirJsModuleGetClassCallChecker,
        FirJsNoRuntimeClassReferenceChecker,
    )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = setOf(
        FirJsModuleQualifiedAccessChecker,
        FirJsDynamicCallChecker
    )

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = setOf(
        FirJsNoRuntimeTypeOperatorChecker,
    )
}
