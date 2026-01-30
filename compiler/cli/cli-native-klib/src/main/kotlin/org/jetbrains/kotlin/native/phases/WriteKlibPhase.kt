/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput
import org.jetbrains.kotlin.native.KlibWriterInput
import org.jetbrains.kotlin.native.writeKlib

public val WriteKlibPhase = createSimpleNamedCompilerPhase<PhaseContext, KlibWriterInput>(
        "WriteKlib",
) { context, input ->
    context.writeKlib(input)
}

public fun <T : PhaseContext> PhaseEngine<T>.writeKlib(
        serializationOutput: SerializerOutput,
        outputPath: String,
        produceHeaderKlib: Boolean = false,
) {
    this.runPhase(WriteKlibPhase, KlibWriterInput(serializationOutput, outputPath, produceHeaderKlib))
}
