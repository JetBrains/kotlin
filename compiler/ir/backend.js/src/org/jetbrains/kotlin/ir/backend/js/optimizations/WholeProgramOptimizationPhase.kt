/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations

import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.backend.js.JsIrOptimizationContext

/**
 * Adapts an optimization over a linked-program input to the compiler phase infrastructure.
 *
 * [NamedCompilerPhase] supplies configuration, validation, and profiling around [body]. When the
 * phase is disabled through [PhaseConfig], [notEnabledOutput] produces the value passed to later
 * phases without executing the optimization.
 */
internal class WholeProgramOptimizationPhase<Input, Output>(
    name: String,
    private val notEnabledOutput: (JsIrOptimizationContext, Input) -> Output,
    private val body: (JsIrOptimizationContext, Input) -> Output,
) : NamedCompilerPhase<JsIrOptimizationContext, Input, Output>(name) {
    override fun phaseBody(context: JsIrOptimizationContext, input: Input): Output =
        body(context, input)

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfig,
        phaserState: PhaserState,
        context: JsIrOptimizationContext,
        input: Input,
    ): Output = notEnabledOutput(context, input)
}
