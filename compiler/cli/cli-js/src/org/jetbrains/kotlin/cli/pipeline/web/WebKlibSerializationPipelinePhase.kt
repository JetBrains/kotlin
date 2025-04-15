/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget

object WebKlibSerializationPipelinePhase : PipelinePhase<JsFir2IrPipelineArtifact, JsSerializedKlibPipelineArtifact>(
    name = "JsKlibSerializationPipelinePhase",
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished)
) {
    override fun executePhase(input: JsFir2IrPipelineArtifact): JsSerializedKlibPipelineArtifact? {
        val (fir2IrResult, firOutput, configuration, diagnosticCollector, moduleStructure, hasErrors) = input

        val outputKlibPath = configuration.computeOutputKlibPath()
        serializeFirKlib(
            moduleStructure = moduleStructure,
            firOutputs = firOutput.output,
            fir2IrActualizedResult = fir2IrResult,
            outputKlibPath = outputKlibPath,
            nopack = configuration.produceKlibDir,
            messageCollector = configuration.messageCollector,
            diagnosticsReporter = diagnosticCollector,
            jsOutputName = configuration.perModuleOutputName,
            useWasmPlatform = configuration.wasmCompilation,
            wasmTarget = configuration.wasmTarget,
            hasErrors = hasErrors,
        )
        return JsSerializedKlibPipelineArtifact(
            outputKlibPath,
            diagnosticCollector,
            configuration
        )
    }

    fun serializeFirKlib(
        moduleStructure: ModulesStructure,
        firOutputs: List<ModuleCompilerAnalyzedOutput>,
        fir2IrActualizedResult: Fir2IrActualizedResult,
        outputKlibPath: String,
        nopack: Boolean,
        messageCollector: MessageCollector,
        diagnosticsReporter: BaseDiagnosticsCollector,
        jsOutputName: String?,
        useWasmPlatform: Boolean,
        wasmTarget: WasmTarget?,
        hasErrors: Boolean = messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
    ) {
        val fir2KlibMetadataSerializer = Fir2KlibMetadataSerializer(
            moduleStructure.compilerConfiguration,
            firOutputs,
            fir2IrActualizedResult,
            exportKDoc = false,
            produceHeaderKlib = false,
        )
        val icData = moduleStructure.compilerConfiguration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles)

        serializeModuleIntoKlib(
            moduleName = moduleStructure.compilerConfiguration[CommonConfigurationKeys.MODULE_NAME]!!,
            configuration = moduleStructure.compilerConfiguration,
            diagnosticReporter = diagnosticsReporter,
            metadataSerializer = fir2KlibMetadataSerializer,
            klibPath = outputKlibPath,
            dependencies = moduleStructure.klibs.all,
            moduleFragment = fir2IrActualizedResult.irModuleFragment,
            irBuiltIns = fir2IrActualizedResult.irBuiltIns,
            cleanFiles = icData ?: emptyList(),
            nopack = nopack,
            containsErrorCode = hasErrors,
            jsOutputName = jsOutputName,
            builtInsPlatform = if (useWasmPlatform) BuiltInsPlatform.WASM else BuiltInsPlatform.JS,
            wasmTarget = wasmTarget,
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
