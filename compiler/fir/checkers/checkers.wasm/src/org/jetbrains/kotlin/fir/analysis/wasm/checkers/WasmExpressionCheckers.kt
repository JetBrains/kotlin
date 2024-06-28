/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.expression.FirWasmDefinedExternallyCallChecker
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.expression.FirWasmExternalRttiChecker
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.expression.FirWasmJsCodeCallChecker
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.expression.FirWasmReifiedExternalChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsCodeConstantArgumentChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirJsQualifierChecker

object WasmBaseExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            FirWasmDefinedExternallyCallChecker,
            FirWasmExternalRttiChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirWasmReifiedExternalChecker
        )
}

object WasmJsExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            FirJsQualifierChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirJsCodeConstantArgumentChecker,
            FirWasmJsCodeCallChecker,
        )
}