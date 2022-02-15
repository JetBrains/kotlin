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
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.createSessionForTests
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger
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
            compileFilesUsingFrontendIR(project, files, configuration, classBuilderFactory, packagePartProvider)
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

    @OptIn(ObsoleteTestInfrastructure::class)
    private fun compileFilesUsingFrontendIR(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
    ): GenerationState {
        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val scope = GlobalSearchScope.filesScope(project, files.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val session = createSessionForTests(project, scope, librariesScope, "main", getPackagePartProvider = packagePartProvider)

        // TODO: add running checkers and check that it's safe to compile
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            configuration.languageVersionSettings,
            files,
            emptyList(),
            IrGenerationExtension.getInstances(project)
        )
        val extensions = JvmGeneratorExtensionsImpl(configuration)
        val (moduleFragment, symbolTable, components) = firAnalyzerFacade.convertToIr(extensions)
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        val codegenFactory = JvmIrCodegenFactory(
            configuration,
            configuration.get(CLIConfigurationKeys.PHASE_CONFIG),
            jvmGeneratorExtensions = extensions
        )

        val generationState = GenerationState.Builder(
            project, classBuilderFactory, moduleFragment.descriptor, dummyBindingContext, configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).build()

        generationState.beforeCompile()
        generationState.oldBEInitTrace(files)
        codegenFactory.generateModuleInFrontendIRMode(
            generationState, files, moduleFragment, symbolTable, extensions, FirJvmBackendExtension(session, components),
        ) {}

        generationState.factory.done()
        return generationState
    }

    fun messageCollectorLogger(collector: MessageCollector) = object : Logger {
        override fun warning(message: String) = collector.report(CompilerMessageSeverity.STRONG_WARNING, message)
        override fun error(message: String) = collector.report(CompilerMessageSeverity.ERROR, message)
        override fun log(message: String) = collector.report(CompilerMessageSeverity.LOGGING, message)
        override fun fatal(message: String): Nothing {
            collector.report(CompilerMessageSeverity.ERROR, message)
            (collector as? GroupingMessageCollector)?.flush()
            kotlin.error(message)
        }
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

        return generateFiles(project, files, configuration, classBuilderFactory, analysisResult)
    }

    fun generateFiles(
        project: Project,
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        classBuilderFactory: ClassBuilderFactory,
        analysisResult: AnalysisResult,
        configureGenerationState: GenerationState.Builder.() -> Unit = {},
    ): GenerationState {
        val isIrBackend =
            (classBuilderFactory.classBuilderMode == ClassBuilderMode.FULL && configuration.getBoolean(JVMConfigurationKeys.IR)) ||
                    configuration.getBoolean(JVMConfigurationKeys.USE_KAPT_WITH_JVM_IR)
        val generationState = GenerationState.Builder(
            project, classBuilderFactory, analysisResult.moduleDescriptor, analysisResult.bindingContext,
            configuration
        ).isIrBackend(isIrBackend).apply(configureGenerationState).build()
        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(
                files,
                generationState,
                if (isIrBackend)
                    JvmIrCodegenFactory(configuration, configuration.get(CLIConfigurationKeys.PHASE_CONFIG))
                else DefaultCodegenFactory
            )
        }
        return generationState
    }
}
