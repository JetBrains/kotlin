/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.native.Fir2IrOutput

public val K2SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PhaseContext, Fir2IrOutput>(
        "SpecialBackendChecks",
) { context, input ->
    val moduleFragment = input.fir2irActualizedResult.irModuleFragment
    SpecialBackendChecksTraversal(
            context,
            input.symbols,
            input.fir2irActualizedResult.irBuiltIns,
    ).lower(moduleFragment)
}

public fun <T : PhaseContext> PhaseEngine<T>.runK2SpecialBackendChecks(fir2IrOutput: Fir2IrOutput) {
    runPhase(K2SpecialBackendChecksPhase, fir2IrOutput)
}