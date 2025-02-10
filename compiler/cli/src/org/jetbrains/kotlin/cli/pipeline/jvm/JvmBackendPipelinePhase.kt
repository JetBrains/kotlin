/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.buildFile
import org.jetbrains.kotlin.cli.common.moduleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.toBackendInput
import org.jetbrains.kotlin.cli.jvm.compiler.createConfigurationForModule
import org.jetbrains.kotlin.cli.jvm.compiler.getSourceFiles
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutputsIfNeeded
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

object JvmBackendPipelinePhase : PipelinePhase<JvmFir2IrPipelineArtifact, JvmBinaryPipelineArtifact>(
    name = "JvmBackendPipelineStep",
    preActions = setOf(
        PerformanceNotifications.GenerationStarted,
        PerformanceNotifications.IrLoweringStarted
    ),
    postActions = setOf(
        PerformanceNotifications.BackendFinished,
        PerformanceNotifications.GenerationFinished,
        CheckCompilationErrors.CheckDiagnosticCollector
    )
) {
    override fun executePhase(input: JvmFir2IrPipelineArtifact): JvmBinaryPipelineArtifact? {
        return executePhase(input, ignoreErrors = false)
    }

    fun executePhase(input: JvmFir2IrPipelineArtifact, ignoreErrors: Boolean): JvmBinaryPipelineArtifact? {
        val (fir2IrResult, configuration, environment, diagnosticCollector, allSourceFiles, mainClassFqName) = input
        val moduleDescriptor = fir2IrResult.irModuleFragment.descriptor
        val project = environment.project
        val classResolver = FirJvmBackendClassResolver(fir2IrResult.components)
        val jvmBackendExtension = FirJvmBackendExtension(
            fir2IrResult.components,
            fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
        )
        val baseBackendInput = fir2IrResult.toBackendInput(configuration, jvmBackendExtension)
        val codegenFactory = JvmIrCodegenFactory(configuration)

        val chunk = configuration.moduleChunk!!.modules
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val codegenInputs = ArrayList<JvmIrCodegenFactory.CodegenInput>(chunk.size)

        val buildFile = configuration.buildFile
        for (module in chunk) {
            val configurationForModule = when {
                chunk.size == 1 -> configuration
                else -> configuration.createConfigurationForModule(module, buildFile)
            }

            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            val backendInput = when (configurationForModule.useLightTree) {
                true -> when (chunk.size) {
                    1 -> baseBackendInput
                    else -> {
                        val wholeModule = baseBackendInput.irModuleFragment
                        val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor)
                        wholeModule.files.filterTo(moduleCopy.files) { file ->
                            file.fileEntry.name in module.getSourceFiles()
                        }
                        baseBackendInput.copy(irModuleFragment = moduleCopy)
                    }
                }

                false -> {
                    val sourceFiles = module.getSourceFiles(
                        allSourceFiles.asKtFilesList(), localFileSystem,
                        multiModuleChunk = chunk.size > 1, buildFile
                    )
                    codegenFactory.getModuleChunkBackendInput(baseBackendInput, sourceFiles)
                }
            }

            codegenInputs += KotlinToJVMBytecodeCompiler.runLowerings(
                project, configurationForModule, moduleDescriptor, module,
                codegenFactory, backendInput, diagnosticCollector, classResolver, reportGenerationStarted = false
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            outputs += KotlinToJVMBytecodeCompiler.runCodegen(
                input,
                input.state,
                codegenFactory,
                diagnosticCollector,
                input.state.configuration,
                reportGenerationFinished = false,
                reportDiagnosticsToMessageCollector = false, // diagnostics will be reported in CheckCompilationErrors.CheckDiagnosticCollector
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        val success = writeOutputsIfNeeded(
            project,
            configuration,
            configuration.messageCollector,
            hasPendingErrors = diagnosticCollector.hasErrors,
            outputs,
            mainClassFqName
        )

        return JvmBinaryPipelineArtifact(outputs).takeIf { success || ignoreErrors }
    }
}
