/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.linkIr
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmLinkedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.library.isWasmStdlib

object WasmIrLinkingPipelinePhase : PipelinePhase<WebLoadedIrPipelineArtifact, WasmLinkedIrPipelineArtifact>(
    name = "WasmIrLinkingPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrLinkingStarted),
    postActions = setOf(PerformanceNotifications.IrLinkingFinished),
) {
    override fun executePhase(input: WebLoadedIrPipelineArtifact): WasmLinkedIrPipelineArtifact {
        val [loadedIr, modulesStructure, configuration] = input
        val [allModules, context] = linkIr(loadedIr, configuration)
        return WasmLinkedIrPipelineArtifact(
            allModules,
            context,
            modulesStructure.klibs.included?.isWasmStdlib == true,
            configuration
        )
    }
}
