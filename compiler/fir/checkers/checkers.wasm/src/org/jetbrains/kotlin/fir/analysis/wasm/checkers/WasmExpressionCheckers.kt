/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object WasmExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
        )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
        )

    override val callCheckers: Set<FirCallChecker>
        get() = setOf(
        )
}
