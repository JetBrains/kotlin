/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase.runAnalysisHandlerExtensions
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirTestSessionFactoryHelper
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.ir.backend.jvm.loadJvmKlibs
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.FirParser
import java.io.File

object GenerationUtils {
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
        trace: BindingTrace = NoScopeRecordCliBindingTrace(environment.project)
    ): GenerationState =
        compileFiles(files, environment.configuration, classBuilderFactory, environment::createPackagePartProvider, trace)

    @JvmStatic
    @JvmOverloads
    fun compileFiles(
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = NoScopeRecordCliBindingTrace(files.first().project)
    ): GenerationState {
        val project = files.first().project
        return if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            compileFilesUsingFrontendIR(project, files, configuration, classBuilderFactory, packagePartProvider)
        } else {
            compileFilesUsingStandardMode(project, files, configuration, classBuilderFactory, packagePartProvider, trace).first
        }
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    private fun compileFilesUsingFrontendIR(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
    ): GenerationState {
        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        if (runAnalysisHandlerExtensions(project, configuration) == false) {
            throw CompilationErrorException()
        }

        val scope = GlobalSearchScope.filesScope(project, files.map { it.virtualFile })
            .uniteWith(AllJavaSourcesInProjectScope(project))
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = FirTestSessionFactoryHelper.createSessionForTests(
            project, scope, librariesScope, configuration, "main", getPackagePartProvider = packagePartProvider,
        )

        // TODO: add running checkers and check that it's safe to compile
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            files,
            emptyList(),
            FirParser.Psi,
        )

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
        val diagnosticReporter = DiagnosticsCollectorImpl()
        firAnalyzerFacade.runResolution()
        val irGenerationExtensions = configuration.getCompilerExtensions(IrGenerationExtension)
        val (moduleFragment, components, pluginContext, _, _, symbolTable) = firAnalyzerFacade.frontendOutput.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            diagnosticReporter,
            irGenerationExtensions
        )

        val generationState = GenerationState(
            project, moduleFragment.descriptor, configuration, classBuilderFactory,
            jvmBackendClassResolver = FirJvmBackendClassResolver(components),
            diagnosticReporter = diagnosticReporter,
        )
        val backendInput = JvmIrCodegenFactory.BackendInput(
            moduleFragment, pluginContext.irBuiltIns, symbolTable, components.irProviders,
            fir2IrExtensions, FirJvmBackendExtension(components, actualizedExpectDeclarations = null), pluginContext,
        )
        JvmIrCodegenFactory(configuration).generateModule(generationState, backendInput)
        return generationState
    }

    private fun compileFilesUsingStandardMode(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace
    ): Pair<GenerationState, BindingContext> {
        val klibs: List<KotlinLibrary> = loadJvmKlibs(configuration).all

        @Suppress("DEPRECATION_ERROR")
        val analysisResult =
            JvmResolveUtil.analyzeAndCheckForErrors(
                project, files, configuration, packagePartProvider, trace,
                klibList = klibs
            )
        analysisResult.throwIfError()

        return generateFiles(project, files, configuration, classBuilderFactory, analysisResult) to analysisResult.bindingContext
    }

    private fun generateFiles(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        analysisResult: AnalysisResult,
    ): GenerationState {
        val state = GenerationState(project, analysisResult.moduleDescriptor, configuration, classBuilderFactory)
        if (analysisResult.shouldGenerateCode) {
            JvmIrCodegenFactory(configuration).convertAndGenerate(files, state, analysisResult.bindingContext)
        }
        return state
    }
}
