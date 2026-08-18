/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactSingleModule
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease

object WasmSingleModuleIncrementalBuildingPhase :
    PipelinePhase<WebIncrementalCachePipelineArtifact<WasmModuleArtifactSingleModule>, WasmIntermediatePipelineArtifact>(
        name = "WasmSingleModuleIncrementalBuildingPhase",
        preActions = setOf(PerformanceNotifications.BackendStarted),
        postActions = setOf(PerformanceNotifications.BackendFinished),
    ) {
    override fun executePhase(input: WebIncrementalCachePipelineArtifact<WasmModuleArtifactSingleModule>): WasmIntermediatePipelineArtifact? {
        val [icCaches, _, cacheGuard, configuration] = input
        val output = cacheGuard.tryAcquireAndRelease {
            compileIncrementallySingleModule(icCaches, configuration)
        }
        return WasmIntermediatePipelineArtifact(output, cacheGuard, configuration)
    }
}
