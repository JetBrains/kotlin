/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.backend.common.serialization.shouldGoToNextIcRound
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilation
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.progress.IncrementalNextRoundException

fun processIncrementalCompilationRoundIfNeeded(
    configuration: CompilerConfiguration,
    frontendOutput: AllModulesFrontendOutput,
    fir2IrResult: Fir2IrActualizedResult,
) {
    if (!configuration.incrementalCompilation) return
    // TODO: During checking the next round, fir serializer may throw an exception, e.g.
    //      during annotation serialization when it cannot find the removed constant
    //      (see ConstantValueUtils.kt:convertToConstantValues())
    //  This happens because we check the next round before compilation errors.
    //  Test reproducer:  testFileWithConstantRemoved
    //  Issue: https://youtrack.jetbrains.com/issue/KT-58824/
    val shouldGoToNextIcRound = shouldGoToNextIcRound(configuration) {
        Fir2KlibMetadataSerializer(
            configuration,
            frontendOutput.outputs,
            fir2IrResult,
            produceHeaderKlib = false,
        )
    }
    if (shouldGoToNextIcRound) {
        // TODO (KT-73991): investigate the need in this hack
        throw IncrementalNextRoundException()
    }
}
