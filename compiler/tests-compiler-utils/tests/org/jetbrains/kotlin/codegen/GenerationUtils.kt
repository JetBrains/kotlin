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
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
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
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirTestSessionFactoryHelper
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.FirParser
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
        val session = FirTestSessionFactoryHelper.createSessionForTests(
            project,
            scope,
            librariesScope,
            "main",
            getPackagePartProvider = packagePartProvider
        )

        val linkViaSignatures = configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
        // TODO: add running checkers and check that it's safe to compile
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            files,
            emptyList(),
            FirParser.Psi,
        )

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings = configuration.languageVersionSettings,
            diagnosticReporter = diagnosticReporter,
            linkViaSignatures = linkViaSignatures,
            evaluatedConstTracker = configuration
                .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
            inlineConstTracker = configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
            expectActualTracker = configuration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
            allowNonCachedDeclarations = false,
            useIrFakeOverrideBuilder = configuration.getBoolean(CommonConfigurationKeys.USE_IR_FAKE_OVERRIDE_BUILDER),
        )

        val commonMemberStorage = Fir2IrCommonMemberStorage(signatureComposerForJvmFir2Ir(linkViaSignatures), FirJvmKotlinMangler())

        firAnalyzerFacade.runResolution()
        val (moduleFragment, components, pluginContext) = firAnalyzerFacade.result.outputs.single().convertToIr(
            fir2IrExtensions,
            fir2IrConfiguration,
            commonMemberStorage,
            irBuiltIns = null,
            JvmIrMangler,
            FirJvmVisibilityConverter,
            DefaultBuiltIns.Instance,
            ::JvmIrTypeSystemContext
        )
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        val codegenFactory = JvmIrCodegenFactory(
            configuration,
            configuration.get(CLIConfigurationKeys.PHASE_CONFIG),
        )

        val generationState = GenerationState.Builder(
            project, classBuilderFactory, moduleFragment.descriptor, dummyBindingContext, configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).diagnosticReporter(
            diagnosticReporter
        ).build()

        generationState.beforeCompile()
        generationState.oldBEInitTrace(files)
        codegenFactory.generateModuleInFrontendIRMode(
            generationState, moduleFragment, components.symbolTable, components.irProviders,
            fir2IrExtensions, FirJvmBackendExtension(components, irActualizedResult = null), pluginContext,
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
            configuration.getBoolean(JVMConfigurationKeys.IR)
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
