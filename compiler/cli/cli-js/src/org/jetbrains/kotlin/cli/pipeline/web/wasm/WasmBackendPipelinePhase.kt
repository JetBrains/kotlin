/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.linkIr
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebIrLoadingPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime

object WasmBackendPipelinePhase : WebBackendPipelinePhase<WasmBackendPipelineArtifact, List<WasmIrModuleConfiguration>>(
    name = "WasmBackendPipelinePhase",
) {
    override val klibLoadingPhase: WebIrLoadingPipelinePhase
        get() = WasmIrLoadingPipelinePhase

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
                configuration = configuration,
            )
            compileResult
        }
        WasmBackendPipelineArtifact(results, outputDir, configuration)
    }

    override fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): List<WasmIrModuleConfiguration> {
        val fragmentCompiler = when (configuration.wasmCompilationMode()) {
            WasmCompilationMode.MULTI_MODULE -> ::compileIncrementallyMultimodule
            WasmCompilationMode.SINGLE_MODULE -> ::compileIncrementallySingleModule
            WasmCompilationMode.REGULAR -> ::compileIncrementallyWholeWorld
        }
        return fragmentCompiler(icCaches.artifacts, configuration)
    }

    override fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): List<WasmIrModuleConfiguration> {
        val (loadedIr, module, configuration) = loadedIrArtifact
        val irFactory = loadedIr.bultins.irFactory as IrFactoryImplForWasmIC
        val compiler = when (configuration.wasmCompilationMode()) {
            WasmCompilationMode.MULTI_MODULE ->
                WholeWorldMultiModuleCompiler(configuration, irFactory)
            WasmCompilationMode.SINGLE_MODULE ->
                SingleModuleCompiler(configuration, irFactory, isWasmStdlib = module.klibs.included?.isWasmStdlib == true)
            WasmCompilationMode.REGULAR ->
                WholeWorldCompiler(configuration, irFactory)
        }

        val (allModules, context) = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLinking) {
            linkIr(loadedIr, configuration, module.mainModule)
        }

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            compiler.lowerIr(loadedIr, exportedDeclarations = setOf(FqName("main")), allModules, context)
        }

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            compiler.compileIr(loweredIr)
        }
    }
}
