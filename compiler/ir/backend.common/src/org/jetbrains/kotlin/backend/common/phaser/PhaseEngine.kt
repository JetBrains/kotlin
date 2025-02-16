/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.DisposableContext
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState

/**
 * PhaseEngine is the heart of the dynamic compiler driver.
 *
 * Unlike the old static compiler driver that relies on a predefined list of
 * phases, the dynamic one requires the user to write a sequence of phases by hand (thus "dynamic").
 *
 * [PhaseEngine] provides a framework for that by tracking phase configuration and state under the hood and exposing two methods:
 * * [runPhase], well, executes a given phase.
 * * [useContext] creates a child engine with a more specific context that will be cleaned up at the end of the call.
 *
 * This way, [PhaseEngine] forces the user to create more specialized contexts that have a limited lifetime.
 */
class PhaseEngine<Context : LoggingContext>(
    val phaseConfig: PhaseConfig,
    val phaserState: PhaserState,
    val context: Context
) {
    companion object;

    /**
     * Switch to a more specific phase engine.
     */
    inline fun <NewContext, R> useContext(newContext: NewContext, action: (PhaseEngine<NewContext>) -> R): R
            where NewContext : DisposableContext,
                  NewContext : LoggingContext {
        val newEngine = PhaseEngine(phaseConfig, phaserState, newContext)
        try {
            return action(newEngine)
        } finally {
            newContext.dispose()
        }
    }

    /**
     * Create a new [PhaseEngine] instance for an existing context that should not be disposed after the action.
     * This is useful for creating engines for a sub/super context type.
     */
    inline fun <NewContext : LoggingContext, R> newEngine(newContext: NewContext, action: (PhaseEngine<NewContext>) -> R): R {
        val newEngine = PhaseEngine(phaseConfig, phaserState, newContext)
        return action(newEngine)
    }

    fun <Input, Output, P : NamedCompilerPhase<Context, Input, Output>> runPhase(
        phase: P,
        input: Input,
        disable: Boolean = false,
    ): Output {
        if (disable) {
            return phase.outputIfNotEnabled(phaseConfig, phaserState, context, input)
        }
        return phase.invoke(phaseConfig, phaserState, context, input)
    }


    fun <Output, P : NamedCompilerPhase<Context, Unit, Output>> runPhase(phase: P): Output = runPhase(phase, Unit)
}
