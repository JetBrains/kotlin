/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.platform

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.*
import org.jetbrains.kotlin.platform.wasm.WasmTarget

internal class LLWasmCheckersConfiguration(wasmTarget: WasmTarget) : LLPlatformCheckersConfiguration {
    override val declarationCheckers: List<DeclarationCheckers> =
        buildList {
            add(WasmBaseDeclarationCheckers)
            when (wasmTarget) {
                WasmTarget.JS -> add(WasmJsDeclarationCheckers)
                WasmTarget.WASI -> add(WasmWasiDeclarationCheckers)
            }
        }

    override val expressionCheckers: List<ExpressionCheckers> =
        buildList {
            add(WasmBaseExpressionCheckers)
            when (wasmTarget) {
                WasmTarget.JS -> add(WasmJsExpressionCheckers)
                WasmTarget.WASI -> {}
            }
        }

    override val typeCheckers: List<TypeCheckers> = listOf(WasmBaseTypeCheckers)
}
