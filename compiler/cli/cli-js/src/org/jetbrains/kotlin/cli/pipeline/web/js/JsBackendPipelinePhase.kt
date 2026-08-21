/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.JsBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebIrLoadingPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.JsICContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.ic.JsModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.JsSrcFileArtifact
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragments
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.config.artifactConfigurations
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File

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

    override fun compileIncrementally(
        icCaches: List<JsModuleArtifact>,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
        val outputs = configuration
            .artifactConfigurations
            .map { compileIncrementally(icCaches, configuration, it) }
        JsBackendPipelineArtifact(CompilerResult(outputs), configuration)
    }

    private fun compileIncrementally(
        icCaches: List<JsModuleArtifact>,
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): CompilationOutputs {
        val beforeIc2Js = System.currentTimeMillis()

        val jsExecutableProducer = JsExecutableProducer(
            artifactConfiguration,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = icCaches,
        )
        (val outputs = compilationOut, val rebuiltModules = buildModules) = jsExecutableProducer.buildExecutable(outJsProgram = false)
        outputs.writeAll()

        configuration.reportLog("Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
        for ([event, duration] in jsExecutableProducer.getStopwatchLaps()) {
            configuration.reportLog("  $event: ${(duration / 1e6).toInt()}ms")
        }

        for (module in rebuiltModules) {
            configuration.reportLog("IC module builder rebuilt JS for module [${File(module).name}]")
        }
        return outputs
    }

    override fun createCacheUpdater(
        cacheDirectory: String,
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration
    ) = CacheUpdater(
        cacheDir = cacheDirectory,
        compilerConfiguration = configuration,
        artifactConfiguration = artifactConfiguration,
        icContext = JsICContext(artifactConfiguration.granularity),
        checkForClassStructuralChanges = false,
        loadBodiesOnlyForMainModule = false,
    )

    override fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): JsBackendPipelineArtifact? {
        val start = System.currentTimeMillis()
        val loweredIr = JsIrLoweringPipelinePhase.executePhaseIsolatedWithActions(loadedIrArtifact) ?: return null
        val output = JsCodegenPipelinePhase.executePhaseIsolatedWithActions(loweredIr) ?: return null
        loadedIrArtifact.configuration.reportLog("Executable production duration: ${System.currentTimeMillis() - start}ms")
        for (outputs in output.result.values) {
            outputs.writeAll()
        }
        return output
    }

    override fun compileIntermediate(
        intermediateResult: JsBackendPipelineArtifact,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact = intermediateResult
}
