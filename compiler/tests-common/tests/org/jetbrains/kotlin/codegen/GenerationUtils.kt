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
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import java.io.File

object GenerationUtils {
    @JvmStatic
    fun compileFileTo(ktFile: KtFile, environment: KotlinCoreEnvironment, output: File): ClassFileFactory =
        compileFilesTo(listOf(ktFile), environment, output)

    @JvmStatic
    fun compileFilesTo(files: List<KtFile>, environment: KotlinCoreEnvironment, output: File): ClassFileFactory =
        compileFiles(files, environment).factory.apply {
            writeAllTo(output)
        }

    @JvmStatic
    @JvmOverloads
    fun compileFiles(
        files: List<KtFile>,
        environment: KotlinCoreEnvironment,
        classBuilderFactory: ClassBuilderFactory = ClassBuilderFactories.TEST,
        trace: BindingTrace = NoScopeRecordCliBindingTrace()
    ): GenerationState =
        compileFiles(files, environment.configuration, classBuilderFactory, environment::createPackagePartProvider, trace)

    @JvmStatic
    @JvmOverloads
    fun compileFiles(
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = NoScopeRecordCliBindingTrace()
    ): GenerationState {
        val analysisResult =
            JvmResolveUtil.analyzeAndCheckForErrors(files.first().project, files, configuration, packagePartProvider, trace)
        analysisResult.throwIfError()

        /* Currently Kapt3 only works with the old JVM backend, so disable IR for everything except actual bytecode generation. */
        val isIrBackend = classBuilderFactory.classBuilderMode == ClassBuilderMode.FULL && configuration.getBoolean(JVMConfigurationKeys.IR)
        val state = GenerationState.Builder(
            files.first().project, classBuilderFactory, analysisResult.moduleDescriptor, analysisResult.bindingContext,
            files, configuration
        ).codegenFactory(
            if (isIrBackend)
                JvmIrCodegenFactory(configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases))
            else DefaultCodegenFactory
        ).isIrBackend(isIrBackend).build()
        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)
        }

        // For JVM-specific errors
        try {
            AnalyzingUtils.throwExceptionOnErrors(state.collectedExtraJvmDiagnostics)
        } catch (e: Throwable) {
            throw TestsCompiletimeError(e)
        }

        return state
    }
}
