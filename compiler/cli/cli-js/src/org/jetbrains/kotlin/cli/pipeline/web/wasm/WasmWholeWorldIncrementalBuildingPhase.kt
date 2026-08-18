/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease

object WasmWholeWorldIncrementalBuildingPhase :
    PipelinePhase<WebIncrementalCachePipelineArtifact<WasmModuleArtifact>, WasmIntermediatePipelineArtifact>(
        name = "WasmWholeWorldIncrementalBuildingPhase",
        preActions = setOf(PerformanceNotifications.BackendStarted),
        postActions = setOf(PerformanceNotifications.BackendFinished),
    ) {

    override fun executePhase(input: WebIncrementalCachePipelineArtifact<WasmModuleArtifact>): WasmIntermediatePipelineArtifact {
        val [icCaches, _, cacheGuard, configuration] = input
        val output = cacheGuard.tryAcquireAndRelease {
            compileIncrementallyWholeWorld(icCaches, configuration)
        }
        return WasmIntermediatePipelineArtifact(output, input.cacheGuard, configuration)
    }
}
