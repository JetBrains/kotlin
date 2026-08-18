/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmLinkedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.library.isWasmStdlib

object WasmIrLinkingPipelinePhase : PipelinePhase<WebLoadedIrPipelineArtifact, WasmLinkedIrPipelineArtifact>(
    name = "WasmIrLinkingPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrLinkingStarted),
    postActions = setOf(PerformanceNotifications.IrLinkingFinished),
) {
    override fun executePhase(input: WebLoadedIrPipelineArtifact): WasmLinkedIrPipelineArtifact {
        val [loadedIr, modulesStructure, configuration] = input
        (val moduleFragment = module, val moduleDependencies = dependencies, val irBuiltIns = bultins, val symbolTable, val irLinker = deserializer) = loadedIr
        val context = WasmBackendContext(
            irBuiltIns = irBuiltIns,
            symbolTable = symbolTable,
            irModuleFragment = moduleFragment,
            configuration = configuration,
        )
        // Create stubs
        ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
        // Sort dependencies after IR linkage.
        val sortedModuleDependencies = irLinker.moduleDependencyTracker.reverseTopoOrder(moduleDependencies)
        val allModules = sortedModuleDependencies.all
        allModules.forEach { it.patchDeclarationParents() }
        irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)
        irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")
        irLinker.clear()
        return WasmLinkedIrPipelineArtifact(
            allModules,
            context,
            modulesStructure.klibs.included?.isWasmStdlib == true,
            configuration
        )
    }
}
