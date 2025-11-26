/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.native.FirSerializerInput
import org.jetbrains.kotlin.native.KlibWriterInput
import org.jetbrains.kotlin.native.fir2IrSerializer
import org.jetbrains.kotlin.native.writeKlib

/**
 * Serializer phase for Native klib compilation.
 *
 * This phase serializes IR to klib format and writes the output.
 */
object NativeKlibSerializerPhase : PipelinePhase<NativeKlibFir2IrArtifact, NativeKlibSerializerArtifact>(
    name = "NativeKlibSerializerPhase",
) {
    override fun executePhase(input: NativeKlibFir2IrArtifact): NativeKlibSerializerArtifact? {
        val phaseContext = input.phaseContext
        val fir2IrOutput = input.fir2IrOutput
        val config = phaseContext.config

        return try {
            // Serialize to klib
            val serializerInput = FirSerializerInput(
                firToIrOutput = fir2IrOutput,
                produceHeaderKlib = false,
            )
            val serializerOutput = phaseContext.fir2IrSerializer(serializerInput)

            // Determine output path
            val outputPath = config.outputPath
            val suffix = CompilerOutputKind.LIBRARY.suffix(config.target)
            val klibOutputFileName = if (outputPath.endsWith(suffix)) outputPath else "$outputPath$suffix"

            // Write the klib
            val writerInput = KlibWriterInput(
                serializerOutput = serializerOutput,
                customOutputPath = null,
                produceHeaderKlib = false,
            )
            phaseContext.writeKlib(writerInput, klibOutputFileName, suffix)

            // Write header klib if configured
            val headerKlibPath = config.headerKlibPath
            if (headerKlibPath != null) {
                val headerSerializerInput = FirSerializerInput(
                    firToIrOutput = fir2IrOutput,
                    produceHeaderKlib = true,
                )
                val headerSerializerOutput = phaseContext.fir2IrSerializer(headerSerializerInput)
                val headerWriterInput = KlibWriterInput(
                    serializerOutput = headerSerializerOutput,
                    customOutputPath = headerKlibPath,
                    produceHeaderKlib = true,
                )
                val headerKlibOutputFileName = if (headerKlibPath.endsWith(suffix)) headerKlibPath else "$headerKlibPath$suffix"
                phaseContext.writeKlib(headerWriterInput, headerKlibOutputFileName, suffix)
            }

            NativeKlibSerializerArtifact(
                phaseContext = phaseContext,
                serializerOutput = serializerOutput,
                outputKlibPath = klibOutputFileName,
            )
        } catch (e: KonanCompilationException) {
            // Serialization errors are already reported
            null
        }
    }
}
