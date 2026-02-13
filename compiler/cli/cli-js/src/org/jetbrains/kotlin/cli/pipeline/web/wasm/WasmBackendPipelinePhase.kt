/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.preserveIcOrder
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly

object WasmBackendPipelinePhase : WebBackendPipelinePhase<WasmBackendPipelineArtifact, List<WasmIrModuleConfiguration>>("WasmBackendPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.WASM_CONFIG_FILES

    override fun compileIntermediate(
        intermediateResult: List<WasmIrModuleConfiguration>,
        configuration: CompilerConfiguration,
    ): WasmBackendPipelineArtifact = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
        val outputDir = configuration.outputDir!!
        val results = intermediateResult.map { result ->
            val linkedModule = linkWasmIr(result)
            val compileResult = compileWasmIrToBinary(result, linkedModule)
            writeCompilationResult(
                result = compileResult,
                dir = outputDir,
                fileNameBase = result.baseFileName,
            )
            compileResult
        }
        WasmBackendPipelineArtifact(results, outputDir, configuration)
    }

    override fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): List<WasmIrModuleConfiguration>? {
        if (configuration.getBoolean(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY)) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Incremental compilation not supported for single module mode"
            )
            return null
        }

        val wasmArtifacts = icCaches.artifacts
            .filterIsInstance<WasmModuleArtifact>()
            .flatMap { it.fileArtifacts }
            .mapNotNull { it.loadIrFragments()?.mainFragment }

        val configuration = WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmArtifacts,
            moduleName = configuration.moduleName!!,
            configuration = configuration,
            typeScriptFragment = null,
            baseFileName = configuration.outputName!!,
            multimoduleOptions = null,
        )
        return listOf(configuration)
    }

    override fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): List<WasmIrModuleConfiguration> {
        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())
        val compiler = when {
            configuration.wasmIncludedModuleOnly ->
                SingleModuleCompiler(configuration, irFactory, isWasmStdlib = module.klibs.included?.isWasmStdlib == true)
            configuration.wasmGenerateClosedWorldMultimodule ->
                WholeWorldMultiModuleCompiler(configuration, irFactory)
            else ->
                WholeWorldCompiler(configuration, irFactory)
        }

        val loadedIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            compiler.loadIr(module)
        }

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            compiler.lowerIr(loadedIr, module.mainModule, exportedDeclarations = setOf(FqName("main")))
        }

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            compiler.compileIr(loweredIr)
        }
    }
}