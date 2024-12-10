/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.js.klib.AnalyzedFirOutput
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilation
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringContext
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.shouldGoToNextIcRound
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.wasm.config.wasmTarget

object WebKlibSerializationPipelinePhase : PipelinePhase<JsFir2IrPipelineArtifact, JsSerializedKlibPipelineArtifact>(
    name = "JsKlibSerializationPipelinePhase",
    preActions = setOf(PerformanceNotifications.GenerationStarted, PerformanceNotifications.IrGenerationStarted),
    postActions = setOf(PerformanceNotifications.GenerationFinished, PerformanceNotifications.IrGenerationFinished)
) {
    override fun executePhase(input: JsFir2IrPipelineArtifact): JsSerializedKlibPipelineArtifact? {
        val (fir2IrResult, firOutput, configuration, diagnosticCollector, moduleStructure) = input

        processIncrementalCompilationRoundIfNeeded(configuration, moduleStructure, firOutput, fir2IrResult)

        val transformedResult = if (configuration.wasmCompilation) {
            fir2IrResult
        } else {
            PhaseEngine(
                configuration.phaseConfig!!,
                PhaserState(),
                JsPreSerializationLoweringContext(fir2IrResult.irBuiltIns, configuration),
            ).runPreSerializationLoweringPhases(fir2IrResult, JsPreSerializationLoweringPhasesProvider, configuration)
        }

        val outputKlibPath = configuration.computeOutputKlibPath()
        serializeFirKlib(
            moduleStructure = moduleStructure,
            firOutputs = firOutput.output,
            fir2IrActualizedResult = transformedResult,
            outputKlibPath = outputKlibPath,
            nopack = configuration.produceKlibDir,
            messageCollector = configuration.messageCollector,
            diagnosticsReporter = diagnosticCollector,
            jsOutputName = configuration.perModuleOutputName,
            useWasmPlatform = configuration.wasmCompilation,
            wasmTarget = configuration.wasmTarget
        )
        return JsSerializedKlibPipelineArtifact(
            outputKlibPath,
            diagnosticCollector,
            configuration
        )
    }

    private fun processIncrementalCompilationRoundIfNeeded(
        configuration: CompilerConfiguration,
        moduleStructure: ModulesStructure,
        firOutput: AnalyzedFirOutput,
        fir2IrResult: Fir2IrActualizedResult,
    ) {
        if (!configuration.incrementalCompilation) return
        // TODO: During checking the next round, fir serializer may throw an exception, e.g.
        //      during annotation serialization when it cannot find the removed constant
        //      (see ConstantValueUtils.kt:convertToConstantValues())
        //  This happens because we check the next round before compilation errors.
        //  Test reproducer:  testFileWithConstantRemoved
        //  Issue: https://youtrack.jetbrains.com/issue/KT-58824/
        val shouldGoToNextIcRound = shouldGoToNextIcRound(moduleStructure.compilerConfiguration) {
            Fir2KlibMetadataSerializer(
                moduleStructure.compilerConfiguration,
                firOutput.output,
                fir2IrResult,
                exportKDoc = false,
                produceHeaderKlib = false,
            )
        }
        if (shouldGoToNextIcRound) {
            // TODO (KT-73991): investigate the need in this hack
            throw IncrementalNextRoundException()
        }
    }
}

fun CompilerConfiguration.computeOutputKlibPath(): String {
    return if (produceKlibFile) {
        outputDir!!.resolve("${outputName!!}.klib").normalize().absolutePath
    } else {
        outputDir!!.absolutePath
    }
}
