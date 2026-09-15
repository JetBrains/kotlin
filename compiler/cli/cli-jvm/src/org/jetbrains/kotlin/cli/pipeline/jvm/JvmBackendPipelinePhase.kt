/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase

@Deprecated(
    "This phase is deprecated. Consider using two new phases instead: `JvmLoweringsPipelinePhase` and `JvmCodegenPipelinePhase`",
    level = WARNING
)
object JvmBackendPipelinePhase : PipelinePhase<JvmFir2IrPipelineArtifact, JvmBackendPipelineArtifact>(
    name = "JvmBackendPipelineStep",
    postActions = setOf(
        CheckCompilationErrors.CheckDiagnosticCollector
    )
) {
    override fun executePhase(input: JvmFir2IrPipelineArtifact): JvmBackendPipelineArtifact {
        val loweringsOutput = JvmLoweringsPipelinePhase.executePhase(input)
        return JvmCodegenPipelinePhase.executePhase(loweringsOutput)
    }
}
