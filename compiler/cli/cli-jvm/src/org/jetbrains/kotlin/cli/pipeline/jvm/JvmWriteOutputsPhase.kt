/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutputsIfNeeded
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.messageCollector

object JvmWriteOutputsPhase : PipelinePhase<JvmBackendPipelineArtifact, JvmBinaryPipelineArtifact>(
    name = "JvmWriteOutputsPhase",
) {
    override fun executePhase(input: JvmBackendPipelineArtifact): JvmBinaryPipelineArtifact {
        val (configuration, environment, mainClassFqName, outputs) = input
        writeOutputsIfNeeded(
            environment.project,
            configuration,
            configuration.messageCollector,
            hasPendingErrors = configuration.diagnosticsCollector.hasErrors,
            outputs,
            mainClassFqName
        )
        return JvmBinaryPipelineArtifact(outputs, configuration)
    }
}
