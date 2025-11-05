/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.wasm.config.wasmTarget

object WebKlibSerializationPipelinePhase : PipelinePhase<JsFir2IrPipelineArtifact, JsSerializedKlibPipelineArtifact>(
    name = "JsKlibSerializationPipelinePhase",
) {
    override fun executePhase(input: JsFir2IrPipelineArtifact): JsSerializedKlibPipelineArtifact {
        val (fir2IrResult, firOutput, configuration, diagnosticCollector, moduleStructure) = input
        val irDiagnosticReporter =
            KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticCollector.deduplicating(), configuration.languageVersionSettings)

        val outputKlibPath = configuration.computeOutputKlibPath()
        val fir2KlibMetadataSerializer = Fir2KlibMetadataSerializer(
            moduleStructure.compilerConfiguration,
            firOutputs = firOutput.output,
            fir2IrActualizedResult = fir2IrResult,
            exportKDoc = false,
            produceHeaderKlib = false,
        )
        val icData =
            moduleStructure.compilerConfiguration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles)
        serializeModuleIntoKlib(
            moduleName = moduleStructure.compilerConfiguration[CommonConfigurationKeys.MODULE_NAME]!!,
            configuration = moduleStructure.compilerConfiguration,
            diagnosticReporter = irDiagnosticReporter,
            metadataSerializer = fir2KlibMetadataSerializer,
            klibPath = outputKlibPath,
            dependencies = moduleStructure.klibs.all,
            moduleFragment = fir2IrResult.irModuleFragment,
            irBuiltIns = fir2IrResult.irBuiltIns,
            cleanFiles = icData ?: emptyList(),
            nopack = configuration.produceKlibDir,
            jsOutputName = configuration.perModuleOutputName,
            builtInsPlatform = if (configuration.wasmCompilation) BuiltInsPlatform.WASM else BuiltInsPlatform.JS,
            wasmTarget = configuration.wasmTarget,
            performanceManager = moduleStructure.compilerConfiguration.perfManager,
        )
        return JsSerializedKlibPipelineArtifact(
            outputKlibPath,
            diagnosticCollector,
            configuration
        )
    }
}

fun CompilerConfiguration.computeOutputKlibPath(): String {
    return if (produceKlibFile) {
        outputDir!!.resolve("${outputName!!}.klib").normalize().absolutePath
    } else {
        outputDir!!.absolutePath
    }
}
