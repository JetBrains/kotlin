/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.cli.common.reportCompilationException
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.JsBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.JsLoweredIrPipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.js.config.artifactConfigurations

object JsCodegenPipelinePhase : PipelinePhase<JsLoweredIrPipelineArtifact, JsBackendPipelineArtifact>(
    name = "JsCodegenPipelinePhase",
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished),
) {
    override fun executePhase(input: JsLoweredIrPipelineArtifact): JsBackendPipelineArtifact? =
        try {
            runCodegen(input)
        } catch (e: CompilationException) {
            input.configuration.reportCompilationException(e)
            null
        }

    private fun runCodegen(input: JsLoweredIrPipelineArtifact): JsBackendPipelineArtifact {
        val configuration = input.configuration
        val transformer = IrModuleToJsTransformer(input.context, input.moduleFragmentToUniqueName)
        val result = transformer.generateModule(
            input.allModules,
            configuration.artifactConfigurations,
            // We only store the JS AST in the result when running compiler tests, otherwise it's not needed,
            // and keeping it wastes memory.
            outJsProgram = configuration.testEnvironment,
        )
        return JsBackendPipelineArtifact(result, configuration)
    }
}
