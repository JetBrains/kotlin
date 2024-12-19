/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.getFileClassInfoFromIrFile
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput.PhasedJvmIrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifactImpl
import org.jetbrains.kotlin.test.frontend.fir.FirOutputPartForDependsOnModule
import org.jetbrains.kotlin.test.java.JavaCompilerFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.SourceFileInfo
import org.jetbrains.kotlin.test.model.TestModule

class PhasedJvmFrontedFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override fun analyze(module: TestModule): FirOutputArtifact {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            rootDisposable = testServices.applicationDisposableProvider.getApplicationRootDisposable(),
        )
        val output = JvmFrontendPipelinePhase.executePhase(input) ?: error("Frontend phase failed")
        val firOutputs = output.result.outputs


        val modulesFromTheSameStructure = buildList {
            add(module)
            module.dependsOnDependencies.mapTo(this) { it.dependencyModule }
        }.associateBy { "<${it.name}>"}
        val testFirOutputs = firOutputs.map {
            val correspondingModule = modulesFromTheSameStructure.getValue(it.session.moduleData.name.asString())
            val testFilePerFirFile = correspondingModule.files.mapNotNull { testFile ->
                val firFile = it.fir.firstOrNull { firFile -> testFile.name == firFile.name } ?: return@mapNotNull null
                testFile to firFile
            }
            FirOutputPartForDependsOnModule(
                module = correspondingModule,
                session = it.session,
                scopeSession = it.scopeSession,
                firAnalyzerFacade = null,
                testFilePerFirFile.toMap()
            )
        }
        return FirOutputArtifactImpl(testFirOutputs, output)
    }
}

class PhasedJvmFir2IrFacade(testServices: TestServices) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact,
    ): IrBackendInput? {
        val input = inputArtifact.phasedOutput!!
        val output = JvmFir2IrPipelinePhase.executePhase(input) ?: return null
        return PhasedJvmIrBackendInput(output)
    }
}

class PhasedJvmIrBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    private val javaCompilerFacade = JavaCompilerFacade(testServices)

    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput
    ): BinaryArtifacts.Jvm? {
        require(inputArtifact is IrBackendInput.PhasedJvmIrBackendInput) {}
        val output = JvmBackendPipelinePhase.executePhase(inputArtifact.input) ?: return null
        val state = output.outputs.single()

        val configuration = inputArtifact.input.configuration
        javaCompilerFacade.compileJavaFiles(module, configuration, state.factory)

        fun sourceFileInfos(irFile: IrFile, allowNestedMultifileFacades: Boolean): List<SourceFileInfo> =
            when (val fileEntry = irFile.fileEntry) {
                is PsiIrFileEntry -> {
                    listOf(
                        SourceFileInfo(
                            KtPsiSourceFile(fileEntry.psiFile),
                            JvmFileClassUtil.getFileClassInfoNoResolve(fileEntry.psiFile as KtFile)
                        )
                    )
                }
                is NaiveSourceBasedFileEntryImpl -> {
                    val sourceFile = inputArtifact.input.sourceFiles.find { it.path == fileEntry.name }
                    if (sourceFile == null) emptyList() // synthetic files, like CoroutineHelpers.kt, are ignored here
                    else listOf(SourceFileInfo(sourceFile, getFileClassInfoFromIrFile(irFile, sourceFile.name)))
                }
                is MultifileFacadeFileEntry -> {
                    if (!allowNestedMultifileFacades) error("nested multi-file facades are not allowed")
                    else fileEntry.partFiles.flatMap { sourceFileInfos(it, allowNestedMultifileFacades = false) }
                }
                else -> {
                    error("unknown kind of file entry: $fileEntry")
                }
            }

        return BinaryArtifacts.Jvm(
            state.factory,
            inputArtifact.irModuleFragment.files.flatMap {
                sourceFileInfos(it, allowNestedMultifileFacades = true)
            }
        )
    }
}
