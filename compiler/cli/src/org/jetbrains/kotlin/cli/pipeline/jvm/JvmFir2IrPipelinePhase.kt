/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object JvmFir2IrPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFir2IrPipelineArtifact>(
    name = "JvmFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrGenerationStarted),
    postActions = setOf(PerformanceNotifications.IrGenerationFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFir2IrPipelineArtifact? {
        val (firResult, configuration, environment, diagnosticCollector, sourceFiles) = input
        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
        val irGenerationExtensions = IrGenerationExtension.Companion.getInstances(environment.project)
        val fir2IrAndIrActualizerResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            diagnosticCollector,
            irGenerationExtensions
        )

        val mainClassFqName = runIf(configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(firResult.outputs.last().fir)
        }

        return JvmFir2IrPipelineArtifact(
            fir2IrAndIrActualizerResult,
            configuration,
            environment,
            diagnosticCollector,
            sourceFiles,
            mainClassFqName,
        )
    }
}
