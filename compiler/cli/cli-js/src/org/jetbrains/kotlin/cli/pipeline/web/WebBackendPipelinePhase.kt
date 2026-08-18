/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.SrcFileArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease
import org.jetbrains.kotlin.js.config.icCacheDirectory
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.util.PhaseType

abstract class WebBackendPipelinePhase<Output, IntermediateOutput, TModuleArtifact, TFileArtifact, TFragments, TBackendContext>(
    name: String,
) : PipelinePhase<ConfigurationPipelineArtifact, Output>(
    name = name,
    preActions = emptySet(),
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) where TFragments : IrICProgramFragments,
        Output : WebBackendPipelineArtifact,
        IntermediateOutput : PipelineArtifact,
        TFileArtifact : SrcFileArtifact,
        TModuleArtifact : ModuleArtifact,
        TBackendContext : JsCommonBackendContext {
    override fun executePhase(input: ConfigurationPipelineArtifact): Output? {
        val configuration = input.configuration

        val cacheDirectory = configuration.icCacheDirectory
        val outputDirPath = configuration.outputDir

        configuration.reportLog("Produce executable: $outputDirPath")
        configuration.reportLog("Cache directory: $cacheDirectory")

        if (cacheDirectory != null) {
            val preparedCachesArtifact = icCachePreparationPhase.executePhaseIsolatedWithActions(input) ?: return null
            val [_, _, cacheGuard, _] = preparedCachesArtifact
            val backendIr = incrementalBuildingPhase.executePhaseIsolatedWithActions(preparedCachesArtifact)
            return cacheGuard.tryAcquireAndRelease {
                backendIr?.let { compileIntermediate(it, configuration) }
            }
        } else {
            configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)
            val loadedKlibArtifact = klibLoadingPhase.executePhaseIsolatedWithActions(input) ?: return null
            val backendIr = compileNonIncrementally(loadedKlibArtifact)
            return backendIr?.let { compileIntermediate(it, configuration) }
        }
    }

    protected abstract val icCachePreparationPhase: WebIncrementalCachePreparationPipelinePhase<TModuleArtifact, *>

    protected abstract val incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<TModuleArtifact>, IntermediateOutput>

    protected abstract val klibLoadingPhase: WebIrLoadingPipelinePhase

    abstract fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): IntermediateOutput?

    abstract fun compileIntermediate(
        intermediateResult: IntermediateOutput,
        configuration: CompilerConfiguration,
    ): Output
}
