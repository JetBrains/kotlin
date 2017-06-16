/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import java.io.File

object GenerationUtils {
    @JvmStatic
    fun compileFileTo(ktFile: KtFile, environment: KotlinCoreEnvironment, output: File): ClassFileFactory =
            compileFile(ktFile, environment).apply {
                writeAllTo(output)
            }

    @JvmStatic
    fun compileFile(ktFile: KtFile, environment: KotlinCoreEnvironment): ClassFileFactory =
            compileFiles(listOf(ktFile), environment).factory

    @JvmStatic
    @JvmOverloads
    fun compileFiles(
            files: List<KtFile>,
            environment: KotlinCoreEnvironment,
            classBuilderFactory: ClassBuilderFactory = ClassBuilderFactories.TEST
    ): GenerationState =
            compileFiles(files, environment.configuration, classBuilderFactory, environment::createPackagePartProvider)

    @JvmStatic
    fun compileFiles(
            files: List<KtFile>,
            configuration: CompilerConfiguration,
            classBuilderFactory: ClassBuilderFactory,
            packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
    ): GenerationState {
        val analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(files.first().project, files, configuration, packagePartProvider)
        analysisResult.throwIfError()

        val state = GenerationState(
                files.first().project, classBuilderFactory, analysisResult.moduleDescriptor, analysisResult.bindingContext,
                files, configuration,
                codegenFactory = if (configuration.getBoolean(JVMConfigurationKeys.IR)) JvmIrCodegenFactory else DefaultCodegenFactory
        )
        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)
        }

        // For JVM-specific errors
        AnalyzingUtils.throwExceptionOnErrors(state.collectedExtraJvmDiagnostics)
        return state
    }
}
