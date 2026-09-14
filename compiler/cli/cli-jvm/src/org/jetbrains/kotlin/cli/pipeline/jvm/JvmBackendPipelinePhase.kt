/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

object JvmBackendPipelinePhase : PipelinePhase<JvmLoweredIrPipelineArtifact, JvmBackendPipelineArtifact>(
    name = JvmBackendPipelinePhase::class.java.name,
    postActions = setOf(
        CheckCompilationErrors.CheckDiagnosticCollector
    )
) {
    override fun executePhase(input: JvmLoweredIrPipelineArtifact): JvmBackendPipelineArtifact {
        val (configuration, environment, mainClassFqName, codegenInputs) = input
        val codegenFactory = JvmIrCodegenFactory(configuration)
        val outputs = ArrayList<GenerationState>(codegenInputs.size)

        for (input in codegenInputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            codegenFactory.invokeCodegen(input)
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            outputs += input.state
        }

        return JvmBackendPipelineArtifact(configuration, environment, mainClassFqName, outputs)
    }
}
