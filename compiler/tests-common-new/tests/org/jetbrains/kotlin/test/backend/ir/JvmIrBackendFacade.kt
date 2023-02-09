/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.getFileClassInfoFromIrFile
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.backend.classic.JavaCompilerFacade
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class JvmIrBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    private val javaCompilerFacade = JavaCompilerFacade(testServices)

    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput
    ): BinaryArtifacts.Jvm? {
        require(inputArtifact is IrBackendInput.JvmIrBackendInput) {
            "JvmIrBackendFacade expects IrBackendInput.JvmIrBackendInput as input"
        }

        if (module.useIrActualizer()) {
            IrActualizer.actualize(
                inputArtifact.backendInput.irModuleFragment,
                inputArtifact.dependentInputs.map { it.irModuleFragment },
                inputArtifact.state.diagnosticReporter,
                inputArtifact.state.languageVersionSettings
            )
        }

        val state = inputArtifact.state
        try {
            inputArtifact.codegenFactory.generateModule(state, inputArtifact.backendInput)
        } catch (e: BackendException) {
            if (CodegenTestDirectives.IGNORE_ERRORS in module.directives) {
                return null
            }
            throw e
        }
        state.factory.done()
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
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
                    val sourceFile = inputArtifact.sourceFiles.find { it.path == fileEntry.name }
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
            inputArtifact.backendInput.irModuleFragment.files.flatMap {
                sourceFileInfos(it, allowNestedMultifileFacades = true)
            }
        )
    }

    private fun TestModule.useIrActualizer(): Boolean {
        return frontendKind == FrontendKinds.FIR && languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
    }
}
