/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.JsBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.ic.JsModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.config.artifactConfigurations
import java.io.File

object JsIncrementalBuildingPhase : PipelinePhase<WebIncrementalCachePipelineArtifact<JsModuleArtifact>, JsBackendPipelineArtifact>(
    name = "JsIncrementalBuildingPhase",
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished),
) {
    override fun executePhase(input: WebIncrementalCachePipelineArtifact<JsModuleArtifact>): JsBackendPipelineArtifact {
        val [icCaches, _, cacheGuard, configuration] = input

        // We use one cache directory for both caches: JS AST and JS code.
        // This guard MUST be unlocked after a successful preparing icCaches (see WebIncrementalCachePreparationPipelinePhase).
        // Do not use IncrementalCacheGuard::acquire() - it may drop an entire cache here, and
        // it breaks the logic from JsExecutableProducer(), therefore use IncrementalCacheGuard::tryAcquire() instead
        // TODO: One day, when we will lower IR and produce JS AST per module,
        //      think about using different directories for JS AST and JS code.
        val outputs = cacheGuard.tryAcquireAndRelease {
            configuration
                .artifactConfigurations
                .map { compileIncrementally(icCaches, configuration, it) }
        }
        return JsBackendPipelineArtifact(CompilerResult(outputs), configuration)
    }

    private fun compileIncrementally(
        icCaches: List<JsModuleArtifact>,
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): JsExecutableProducer.BuildResult {
        val beforeIc2Js = System.currentTimeMillis()

        val jsExecutableProducer = JsExecutableProducer(
            artifactConfiguration,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = icCaches,
        )
        val buildResult = jsExecutableProducer.buildExecutable(outJsProgram = false)
        (val outputs = compilationOut, val rebuiltModules = buildModules) = buildResult
        outputs.writeAll()

        configuration.reportLog("Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
        for ([event, duration] in jsExecutableProducer.getStopwatchLaps()) {
            configuration.reportLog("  $event: ${(duration / 1e6).toInt()}ms")
        }

        for (module in rebuiltModules) {
            configuration.reportLog("IC module builder rebuilt JS for module [${File(module).name}]")
        }
        return buildResult
    }
}
