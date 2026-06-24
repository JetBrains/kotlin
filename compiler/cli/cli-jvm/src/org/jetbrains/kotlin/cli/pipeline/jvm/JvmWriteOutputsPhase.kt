/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.hasMessageCollectorErrors
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.reportOutput
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import java.io.File

object JvmWriteOutputsPhase : PipelinePhase<JvmBackendPipelineArtifact, JvmBinaryPipelineArtifact>(
    name = "JvmWriteOutputsPhase",
) {
    override fun executePhase(input: JvmBackendPipelineArtifact): JvmBinaryPipelineArtifact {
        val (configuration, environment, mainClassFqName, outputs) = input
        writeOutputsIfNeeded(
            environment.project,
            configuration,
            hasPendingErrors = configuration.diagnosticsCollector.hasErrors,
            outputs,
            mainClassFqName
        )
        return JvmBinaryPipelineArtifact(outputs, configuration)
    }

    fun writeOutputsIfNeeded(
        project: Project,
        configuration: CompilerConfiguration,
        hasPendingErrors: Boolean,
        outputs: Collection<GenerationState>,
        mainClassFqName: FqName?
    ): Boolean {
        if (hasPendingErrors || configuration.hasMessageCollectorErrors()) {
            return false
        }

        for (state in outputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            writeOutput(state.configuration, state.factory, mainClassFqName)
        }

        return true
    }

    private fun writeOutput(
        configuration: CompilerConfiguration,
        outputFiles: OutputFileCollection,
        mainClassFqName: FqName?
    ) {
        val reportOutputFiles = configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        val jarPath = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
        if (jarPath != null) {
            val includeRuntime = configuration.get(JVMConfigurationKeys.INCLUDE_RUNTIME, false)
            val noReflect = configuration.get(JVMConfigurationKeys.NO_REFLECT, false)
            val resetJarTimestamps = !configuration.get(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS, false)
            CompileEnvironmentUtil.writeToJar(
                jarPath,
                includeRuntime,
                noReflect,
                resetJarTimestamps,
                mainClassFqName,
                outputFiles,
                configuration
            )
            val sourceFiles = outputFiles.asList().flatMap { it.sourceFiles }.distinct()
            configuration.fileMappingTracker?.recordSourceFilesToOutputFileMapping(
                sourceFiles,
                jarPath
            )
            if (reportOutputFiles) {
                val message = OutputMessageUtil.formatOutputMessage(sourceFiles, jarPath)
                configuration.reportOutput(message)
            }
            return
        }

        outputFiles.writeAll(
            configuration.outputDirOrCurrentDirectory(),
            configuration,
            reportOutputFiles,
            configuration.fileMappingTracker
        )
    }

    private fun CompilerConfiguration.outputDirOrCurrentDirectory(): File {
        return outputDirectory?.takeUnless { it.path.isBlank() } ?: File(".")
    }
}
