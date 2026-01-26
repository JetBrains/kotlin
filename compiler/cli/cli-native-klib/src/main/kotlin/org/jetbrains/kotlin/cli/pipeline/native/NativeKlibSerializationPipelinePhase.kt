/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.native.FirSerializerInput
import org.jetbrains.kotlin.native.KlibWriterInput
import org.jetbrains.kotlin.native.fir2IrSerializer
import org.jetbrains.kotlin.native.writeKlib

object NativeKlibSerializationPipelinePhase : PipelinePhase<NativeFir2IrPipelineArtifact, NativeSerializedKlibPipelineArtifact>(
    name = "NativeKlibSerializationPipelinePhase",
) {
    override fun executePhase(input: NativeFir2IrPipelineArtifact): NativeSerializedKlibPipelineArtifact? {
        val configuration = input.configuration
        val phaseContext = input.phaseContext

        val outputName = configuration.get(KonanConfigKeys.OUTPUT)
            ?: configuration.get(CommonConfigurationKeys.MODULE_NAME)
            ?: "output"
        val outputKlibPath = "$outputName.klib"

        // Produce header klib if requested
        val headerKlibPath = configuration.get(KonanConfigKeys.HEADER_KLIB)
        if (!headerKlibPath.isNullOrEmpty()) {
            val headerSerializerInput = FirSerializerInput(
                firToIrOutput = input.fir2IrOutput,
                produceHeaderKlib = true,
            )
            val headerSerializerOutput = phaseContext.fir2IrSerializer(headerSerializerInput)

            val headerKlibWriterInput = KlibWriterInput(
                serializerOutput = headerSerializerOutput,
                customOutputPath = null,
                produceHeaderKlib = true,
            )
            phaseContext.writeKlib(headerKlibWriterInput, headerKlibPath, ".klib")

            // Don't overwrite the header klib with the full klib and stop compilation here.
            // By providing the same path for both regular output and header klib we can skip emitting the full klib.
            if (File(outputKlibPath).canonicalPath == File(headerKlibPath).canonicalPath) {
                return NativeSerializedKlibPipelineArtifact(
                    serializerOutput = headerSerializerOutput,
                    outputKlibPath = headerKlibPath,
                    diagnosticCollector = input.diagnosticCollector,
                    configuration = configuration,
                )
            }
        }

        // Produce full klib
        val serializerInput = FirSerializerInput(
            firToIrOutput = input.fir2IrOutput,
            produceHeaderKlib = false,
        )
        val serializerOutput = phaseContext.fir2IrSerializer(serializerInput)

        val klibWriterInput = KlibWriterInput(
            serializerOutput = serializerOutput,
            customOutputPath = null,
            produceHeaderKlib = false,
        )
        phaseContext.writeKlib(klibWriterInput, outputKlibPath, ".klib")

        return NativeSerializedKlibPipelineArtifact(
            serializerOutput = serializerOutput,
            outputKlibPath = outputKlibPath,
            diagnosticCollector = input.diagnosticCollector,
            configuration = configuration,
        )
    }
}
