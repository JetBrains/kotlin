/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.buildFile
import org.jetbrains.kotlin.cli.common.moduleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.codegenFactoryWithJvmIrBackendInput
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.codegen.CodegenFactory
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
    preActions = setOf(PerformanceNotifications.IrLoweringsStarted),
    postActions = setOf(PerformanceNotifications.GenerationFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFir2IrPipelineArtifact): JvmBinaryPipelineArtifact? {
        val (fir2IrResult, configuration, environment, diagnosticCollector, allSourceFiles, mainClassFqName) = input
        val moduleDescriptor = fir2IrResult.irModuleFragment.descriptor
        val project = environment.project
        val bindingContext = NoScopeRecordCliBindingTrace(project).bindingContext
        val classResolver = FirJvmBackendClassResolver(fir2IrResult.components)
        val jvmBackendExtension = FirJvmBackendExtension(
            fir2IrResult.components,
            fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
        )
        val (codegenFactory, baseBackendInput) = fir2IrResult.codegenFactoryWithJvmIrBackendInput(configuration, jvmBackendExtension)

        val chunk = configuration.moduleChunk!!.modules
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val codegenInputs = ArrayList<CodegenFactory.CodegenInput>(chunk.size)

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
                project, configurationForModule, moduleDescriptor, bindingContext,
                sourceFiles = null, module, codegenFactory, backendInput, diagnosticCollector,
                classResolver
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            // Codegen (per module)
            outputs += KotlinToJVMBytecodeCompiler.runCodegen(
                input,
                input.state,
                codegenFactory,
                diagnosticCollector,
                input.state.configuration
            )
        }

        val success = writeOutputsIfNeeded(project, configuration, configuration.messageCollector, outputs, mainClassFqName)

        if (!success) return null

        return JvmBinaryPipelineArtifact(outputs)
    }
}
