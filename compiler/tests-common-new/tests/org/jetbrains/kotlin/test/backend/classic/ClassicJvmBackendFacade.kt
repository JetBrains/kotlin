/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.classic

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.SourceFileInfo
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class ClassicJvmBackendFacade(
    testServices: TestServices
) : ClassicBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    private val javaCompilerFacade = JavaCompilerFacade(testServices)

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun transform(
        module: TestModule,
        inputArtifact: ClassicBackendInput
    ): BinaryArtifacts.Jvm {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val (psiFiles, analysisResult, project, _) = inputArtifact
        // TODO: add configuring classBuilderFactory
        val generationState = GenerationState.Builder(
            project,
            ClassBuilderFactories.TEST,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            configuration
        ).build()

        KotlinCodegenFacade.compileCorrectFiles(psiFiles, generationState, DefaultCodegenFactory)
        javaCompilerFacade.compileJavaFiles(module, configuration, generationState.factory)
        return BinaryArtifacts.Jvm(
            generationState.factory,
            psiFiles.map { SourceFileInfo(KtPsiSourceFile(it), JvmFileClassUtil.getFileClassInfoNoResolve(it)) }
        )
    }
}
