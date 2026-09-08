/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactSingleModule
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease

abstract class WasmIncrementalBuildingPhase<M : ModuleArtifact>(name: String) :
    PipelinePhase<WebIncrementalCachePipelineArtifact<M>, WasmIntermediatePipelineArtifact>(
        name = name,
        preActions = setOf(PerformanceNotifications.BackendStarted),
        postActions = setOf(PerformanceNotifications.BackendFinished),
    ) {
}

object WasmMultiModuleIncrementalBuildingPhase :
    WasmIncrementalBuildingPhase<WasmModuleArtifactMultimodule>(name = WasmMultiModuleIncrementalBuildingPhase::class.java.simpleName) {
    override fun executePhase(input: WebIncrementalCachePipelineArtifact<WasmModuleArtifactMultimodule>): WasmIntermediatePipelineArtifact {
        val [icCaches, _, cacheGuard, configuration] = input
        val output = cacheGuard.tryAcquireAndRelease {
            compileIncrementallyMultimodule(icCaches, configuration)
        }
        return WasmIntermediatePipelineArtifact(output, cacheGuard, configuration)
    }
}

object WasmSingleModuleIncrementalBuildingPhase :
    WasmIncrementalBuildingPhase<WasmModuleArtifactSingleModule>(name = WasmSingleModuleIncrementalBuildingPhase::class.java.simpleName) {
    override fun executePhase(input: WebIncrementalCachePipelineArtifact<WasmModuleArtifactSingleModule>): WasmIntermediatePipelineArtifact {
        val [icCaches, _, cacheGuard, configuration] = input
        val output = cacheGuard.tryAcquireAndRelease {
            compileIncrementallySingleModule(icCaches, configuration)
        }
        return WasmIntermediatePipelineArtifact(output, cacheGuard, configuration)
    }
}

object WasmWholeWorldIncrementalBuildingPhase :
    WasmIncrementalBuildingPhase<WasmModuleArtifact>(name = WasmWholeWorldIncrementalBuildingPhase::class.java.simpleName) {
    override fun executePhase(input: WebIncrementalCachePipelineArtifact<WasmModuleArtifact>): WasmIntermediatePipelineArtifact {
        val [icCaches, _, cacheGuard, configuration] = input
        val output = cacheGuard.tryAcquireAndRelease {
            compileIncrementallyWholeWorld(icCaches, configuration)
        }
        return WasmIntermediatePipelineArtifact(output, input.cacheGuard, configuration)
    }
}
