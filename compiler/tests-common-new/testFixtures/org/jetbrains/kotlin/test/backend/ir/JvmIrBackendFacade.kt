/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.getFileClassInfoFromIrFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_JAVA_FACADE
import org.jetbrains.kotlin.test.java.JavaCompilerFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.SourceFileInfo
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractJvmIrBackendFacade(testServices: TestServices) : IrBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    private val javaCompilerFacade = JavaCompilerFacade(testServices)

    protected abstract fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState?
    protected abstract val IrBackendInput.sourceFiles: Collection<KtSourceFile>

    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput
    ): BinaryArtifacts.Jvm? {
        val state = try {
            produceGenerationState(inputArtifact)
        } catch (e: BackendException) {
            if (CodegenTestDirectives.IGNORE_ERRORS in module.directives) {
                return null
            }
            throw e
        } ?: return null

        // Currently there's a ton of diagnostic tests with incorrect Java code:
        // strictly speaking, compiling it with javac is not required for testing
        // Kotlin code
        if (DISABLE_JAVA_FACADE !in module.directives) {
            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            javaCompilerFacade.compileJavaFiles(module, configuration, state.factory)
        }

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
            inputArtifact.irModuleFragment.files.flatMap {
                sourceFileInfos(it, allowNestedMultifileFacades = true)
            }
        )
    }
}

class JvmIrBackendFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState {
        require(inputArtifact is IrBackendInput.JvmIrBackendInput) {
            "JvmIrBackendFacade expects IrBackendInput.JvmIrBackendInput as input"
        }
        val state = inputArtifact.state
        inputArtifact.codegenFactory.generateModule(state, inputArtifact.backendInput)
        return state
    }

    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = (this as IrBackendInput.JvmIrBackendInput).sourceFiles
}
