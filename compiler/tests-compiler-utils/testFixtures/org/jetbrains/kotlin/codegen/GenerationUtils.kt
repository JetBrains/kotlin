/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase.runAnalysisHandlerExtensions
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirTestSessionFactoryHelper
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
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
        val project = files.first().project
        return if (configuration.useFir || configuration.languageVersionSettings.languageVersion.usesK2) {
            compileFilesUsingFrontendIR(project, files, configuration, classBuilderFactory, packagePartProvider)
        } else {
            error("K1 compilation is no longer supported")
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

        val fir2IrExtensions = JvmFir2IrExtensions()
        val diagnosticReporter = DiagnosticsCollectorImpl()
        firAnalyzerFacade.runResolution()
        val irGenerationExtensions = configuration.getCompilerExtensions(IrGenerationExtension)
        (
            val moduleFragment = irModuleFragment, val components, val pluginContext, val _ = irActualizedResult, val _ = irBuiltIns, val symbolTable
        ) =
            firAnalyzerFacade.frontendOutput.convertToIrAndActualizeForJvm(
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
            debuggerExtensions = null, FirJvmBackendExtension(components, actualizedExpectDeclarations = null), pluginContext,
        )
        JvmIrCodegenFactory(configuration).generateModule(generationState, backendInput)
        return generationState
    }
}
