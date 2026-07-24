/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory.BackendInput
import org.jetbrains.kotlin.cli.common.buildFile
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.moduleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.createConfigurationForModule
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationUpdater.getBuildFilePaths
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File

object JvmBackendPipelinePhase : PipelinePhase<JvmFir2IrPipelineArtifact, JvmBackendPipelineArtifact>(
    name = "JvmBackendPipelineStep",
    postActions = setOf(
        CheckCompilationErrors.CheckDiagnosticCollector
    )
) {
    override fun executePhase(input: JvmFir2IrPipelineArtifact): JvmBackendPipelineArtifact {
        (val fir2IrResult = result, val configuration, val environment, val allSourceFiles = sourceFiles, val mainClassFqName) = input
        val moduleDescriptor = fir2IrResult.irModuleFragment.descriptor
        val diagnosticsCollector = configuration.diagnosticsCollector
        val project = environment.project
        val classResolver = FirJvmBackendClassResolver(fir2IrResult.components)
        val jvmBackendExtension = FirJvmBackendExtension(
            fir2IrResult.components,
            fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
        )
        val baseBackendInput = with(fir2IrResult) {
            BackendInput(
                irModuleFragment, irBuiltIns, symbolTable, components.irProviders,
                debuggerExtensions = null, jvmBackendExtension, pluginContext
            )
        }

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

            codegenInputs += runLowerings(
                project, configurationForModule, moduleDescriptor, module,
                codegenFactory, backendInput, diagnosticsCollector, classResolver,
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            outputs += runCodegen(
                input,
                input.state,
                codegenFactory,
                diagnosticsCollector,
                input.state.configuration,
                reportDiagnosticsToMessageCollector = false, // diagnostics will be reported in CheckCompilationErrors.CheckDiagnosticCollector
            )
        }

        return JvmBackendPipelineArtifact(configuration, environment, mainClassFqName, outputs)
    }

    val customClassBuilderFactory = CompilerConfigurationKey.create<ClassBuilderFactory>("customClassBuilderFactory")

    fun runLowerings(
        project: Project,
        configuration: CompilerConfiguration,
        moduleDescriptor: ModuleDescriptor,
        module: Module?,
        codegenFactory: JvmIrCodegenFactory,
        backendInput: BackendInput,
        diagnosticsReporter: BaseDiagnosticsCollector,
        backendClassResolver: JvmBackendClassResolver,
    ): JvmIrCodegenFactory.CodegenInput {
        val state = GenerationState(
            project,
            moduleDescriptor,
            configuration,
            builderFactory = configuration.get(customClassBuilderFactory, ClassBuilderFactories.BINARIES),
            targetId = module?.let(::TargetId),
            moduleName = module?.getModuleName() ?: configuration.moduleName,
            diagnosticReporter = diagnosticsReporter,
            jvmBackendClassResolver = backendClassResolver,
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            codegenFactory.invokeLowerings(state, backendInput)
        }
    }

    fun runCodegen(
        codegenInput: JvmIrCodegenFactory.CodegenInput,
        state: GenerationState,
        codegenFactory: JvmIrCodegenFactory,
        diagnosticsReporter: BaseDiagnosticsCollector,
        configuration: CompilerConfiguration,
        reportDiagnosticsToMessageCollector: Boolean,
    ): GenerationState {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        codegenFactory.invokeCodegen(codegenInput)

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (reportDiagnosticsToMessageCollector) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, configuration)
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return state
    }

    fun Module.getSourceFiles(
        allSourceFiles: List<KtFile>,
        localFileSystem: VirtualFileSystem?,
        multiModuleChunk: Boolean,
        buildFile: File?
    ): List<KtFile> {
        return if (multiModuleChunk) {
            // filter out source files from other modules
            assert(buildFile != null) { "Compiling multiple modules, but build file is null" }
            val [moduleSourceDirs, moduleSourceFiles] =
                getBuildFilePaths(buildFile, getSourceFiles())
                    .mapNotNull(localFileSystem!!::findFileByPath)
                    .partition(VirtualFile::isDirectory)

            allSourceFiles.filter { file ->
                val virtualFile = file.virtualFile
                virtualFile in moduleSourceFiles || moduleSourceDirs.any { dir ->
                    VfsUtilCore.isAncestor(dir, virtualFile, true)
                }
            }
        } else {
            allSourceFiles
        }
    }
}
