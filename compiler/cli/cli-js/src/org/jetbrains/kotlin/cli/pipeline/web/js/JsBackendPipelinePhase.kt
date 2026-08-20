/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.*
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ic.JsModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.JsSrcFileArtifact
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragments

object JsBackendPipelinePhase : WebBackendPipelinePhase<
        JsBackendPipelineArtifact,
        JsBackendPipelineArtifact,
        JsModuleArtifact,
        JsSrcFileArtifact,
        JsIrProgramFragments,
        JsIrBackendContext,
        >(name = "JsBackendPipelinePhase") {
    override val klibLoadingPhase: WebIrLoadingPipelinePhase
        get() = JsIrLoadingPipelinePhase

    override val icCachePreparationPhase: WebIncrementalCachePreparationPipelinePhase<JsModuleArtifact, *>
        get() = JsIncrementalCachePreparationPipelinePhase

    override val incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<JsModuleArtifact>, JsBackendPipelineArtifact>
        get() = JsIncrementalBuildingPhase

    override fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): JsBackendPipelineArtifact? {
        val start = System.currentTimeMillis()
        val loweredIr = JsIrLoweringPipelinePhase.executePhaseIsolatedWithActions(loadedIrArtifact) ?: return null
        val output = JsCodegenPipelinePhase.executePhaseIsolatedWithActions(loweredIr) ?: return null
        loadedIrArtifact.configuration.reportLog("Executable production duration: ${System.currentTimeMillis() - start}ms")
        return output
    }

    override fun compileIntermediate(
        intermediateResult: JsBackendPipelineArtifact,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact = intermediateResult
}
