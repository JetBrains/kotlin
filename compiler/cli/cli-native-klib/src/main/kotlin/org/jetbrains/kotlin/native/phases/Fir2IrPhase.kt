/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.native.Fir2IrOutput
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.fir2Ir

public val Fir2IrPhase = createSimpleNamedCompilerPhase(
        "Fir2Ir",
        outputIfNotEnabled = { _, _, _, _ -> error("Fir2Ir phase cannot be disabled") }
) { context: PhaseContext, input: FirOutput.Full ->
    context.fir2Ir(input.firResult)
}

public fun <T : PhaseContext> PhaseEngine<T>.runFir2Ir(input: FirOutput.Full): Fir2IrOutput {
    return this.runPhase(Fir2IrPhase, input)
}
