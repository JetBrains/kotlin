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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.js.messageCollectorLogger
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmClassCodegen
import org.jetbrains.kotlin.fir.createSession
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.util.DummyLogger
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
        val project = files.first().project
        val state = if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            compileFilesUsingFrontendIR(project, files, configuration, classBuilderFactory, packagePartProvider, trace)
        } else {
            compileFilesUsingStandardMode(project, files, configuration, classBuilderFactory, packagePartProvider, trace)
        }

        // For JVM-specific errors
        try {
            AnalyzingUtils.throwExceptionOnErrors(state.collectedExtraJvmDiagnostics)
        } catch (e: Throwable) {
            throw TestsCompiletimeError(e)
        }

        return state
    }

    private fun compileFilesUsingFrontendIR(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace
    ): GenerationState {
        Extensions.getArea(project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)

        val scope = GlobalSearchScope.filesScope(project, files.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = createSession(project, scope, librariesScope, "main", packagePartProvider)

        // TODO: add running checkers and check that it's safe to compile
        val firAnalyzerFacade = FirAnalyzerFacade(session, configuration.languageVersionSettings, files)
        val (moduleFragment, symbolTable, sourceManager, components) = firAnalyzerFacade.convertToIr()
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        val codegenFactory = JvmIrCodegenFactory(configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases))

        // Create and initialize the test module and its dependencies
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
            project, files, trace, configuration, packagePartProvider, ::FileBasedDeclarationProviderFactory, CompilerEnvironment,
            TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, files), emptyList()
        )
        val generationState = GenerationState.Builder(
            project, classBuilderFactory, container.get<ModuleDescriptor>(), dummyBindingContext, files, configuration
        ).codegenFactory(
            codegenFactory
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).build()

        generationState.beforeCompile()
        codegenFactory.generateModuleInFrontendIRMode(
            generationState, moduleFragment, symbolTable, sourceManager
        ) { irClass, context, irFunction ->
            FirJvmClassCodegen(irClass, context, irFunction, session)
        }

        generationState.factory.done()
        return generationState
    }

    private fun compileFilesUsingStandardMode(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace
    ): GenerationState {
        val logger = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)?.let { messageCollectorLogger(it) }
            ?: DummyLogger
        val resolvedKlibs = configuration.get(JVMConfigurationKeys.KLIB_PATHS)?.let { klibPaths ->
            jvmResolveLibraries(klibPaths, logger)
        }

        val analysisResult =
            JvmResolveUtil.analyzeAndCheckForErrors(
                project, files, configuration, packagePartProvider, trace,
                klibList = resolvedKlibs?.getFullList() ?: emptyList()
            )
        analysisResult.throwIfError()

        /* Currently Kapt3 only works with the old JVM backend, so disable IR for everything except actual bytecode generation. */
        val isIrBackend =
            classBuilderFactory.classBuilderMode == ClassBuilderMode.FULL && configuration.getBoolean(JVMConfigurationKeys.IR)
        val generationState = GenerationState.Builder(
            project, classBuilderFactory, analysisResult.moduleDescriptor, analysisResult.bindingContext,
            files, configuration
        ).codegenFactory(
            if (isIrBackend)
                JvmIrCodegenFactory(configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases))
            else DefaultCodegenFactory
        ).isIrBackend(isIrBackend).build()
        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(generationState)
        }
        return generationState
    }
}
