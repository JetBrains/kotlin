/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.convertToIr
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionFactory.createSessionWithDependencies
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

object FirKotlinToJvmBytecodeCompiler {
    fun compileModulesUsingFrontendIR(
        projectEnvironment: AbstractProjectEnvironment,
        projectConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        allSources: List<KtFile>,
        buildFile: File?,
        chunk: List<Module>,
        extendedAnalysisMode: Boolean
    ): Boolean {
        val performanceManager = projectConfiguration.get(CLIConfigurationKeys.PERF_MANAGER)

        messageCollector.report(
            STRONG_WARNING,
            "ATTENTION!\n This build uses in-dev FIR: \n  -Xuse-fir"
        )

        val outputs = ArrayList<Pair<FirResult, GenerationState>>(chunk.size)
        val targetIds = projectConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
        val incrementalComponents = projectConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        val isMultiModuleChunk = chunk.size > 1

        // TODO: run lowerings for all modules in the chunk, then run codegen for all modules.
        val project = (projectEnvironment as? PsiBasedProjectEnvironment)?.project
        for (module in chunk) {
            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)
            val context = CompilationContext(
                module,
                module.getSourceFiles(
                    allSources, (projectEnvironment as? PsiBasedProjectEnvironment)?.localFileSystem, isMultiModuleChunk, buildFile
                ),
                projectEnvironment,
                messageCollector,
                moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME),
                moduleConfiguration,
                performanceManager,
                targetIds,
                incrementalComponents,
                extendedAnalysisMode,
                firExtensionRegistrars = project?.let { FirExtensionRegistrar.getInstances(it) } ?: emptyList(),
                irGenerationExtensions = project?.let { IrGenerationExtension.getInstances(it) } ?: emptyList()
            )
            val generationState = context.compileModule() ?: return false
            outputs += generationState
        }

        val mainClassFqName: FqName? = runIf(chunk.size == 1 && projectConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(outputs.single().first)
        }

        return writeOutputs(
            project,
            projectConfiguration,
            chunk,
            outputs.map(Pair<FirResult, GenerationState>::second),
            mainClassFqName
        )
    }

    private fun CompilationContext.compileModule(): Pair<FirResult, GenerationState>? {
        performanceManager?.notifyAnalysisStarted()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (!checkKotlinPackageUsage(moduleConfiguration, allSources)) return null

        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()
        val firResult = runFrontend(allSources, diagnosticsReporter).also {
            performanceManager?.notifyAnalysisFinished()
        }
        if (firResult == null) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector)
            return null
        }

        performanceManager?.notifyGenerationStarted()
        performanceManager?.notifyIRTranslationStarted()

        val extensions = JvmGeneratorExtensionsImpl(moduleConfiguration)
        val fir2IrResult = firResult.session.convertToIr(firResult.scopeSession, firResult.fir, extensions, irGenerationExtensions)

        performanceManager?.notifyIRTranslationFinished()

        val generationState = runBackend(
            allSources,
            fir2IrResult,
            extensions,
            firResult.session,
            diagnosticsReporter
        )

        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector)

        performanceManager?.notifyIRGenerationFinished()
        performanceManager?.notifyGenerationFinished()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return firResult to generationState
    }

    private class FirResult(
        val session: FirSession,
        val scopeSession: ScopeSession,
        val fir: List<FirFile>
    )

    private fun CompilationContext.runFrontend(ktFiles: List<KtFile>, diagnosticsReporter: BaseDiagnosticsCollector): FirResult? {
        @Suppress("NAME_SHADOWING")
        var ktFiles = ktFiles
        val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
            AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
        }

        var sourceScope = (projectEnvironment as PsiBasedProjectEnvironment).getSearchScopeByPsiFiles(ktFiles) +
                projectEnvironment.getSearchScopeForProjectJavaSources()

        var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

        val providerAndScopeForIncrementalCompilation = createComponentsForIncrementalCompilation(sourceScope)

        providerAndScopeForIncrementalCompilation?.scope?.let {
            librariesScope -= it
        }

        val languageVersionSettings = moduleConfiguration.languageVersionSettings

        val commonKtFiles = ktFiles.filter { it.isCommonSource == true }

        val sessionProvider = FirProjectSessionProvider()

        fun createSession(
            name: String,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices,
            sourceScope: AbstractProjectFileSearchScope,
            dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {}
        ): FirSession {
            return createSessionWithDependencies(
                Name.identifier(name),
                platform,
                analyzerServices,
                externalSessionProvider = sessionProvider,
                projectEnvironment,
                languageVersionSettings,
                sourceScope,
                librariesScope,
                lookupTracker = moduleConfiguration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
                providerAndScopeForIncrementalCompilation,
                firExtensionRegistrars,
                dependenciesConfigurator = {
                    dependencies(moduleConfiguration.jvmClasspathRoots.map { it.toPath() })
                    dependencies(moduleConfiguration.jvmModularRoots.map { it.toPath() })
                    friendDependencies(moduleConfiguration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                    dependenciesConfigurator()
                }
            ) {
                if (extendedAnalysisMode) {
                    registerExtendedCommonCheckers()
                }
            }
        }

        val commonSession = runIf(
            languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) && commonKtFiles.isNotEmpty()
        ) {
            val commonSourcesScope = projectEnvironment.getSearchScopeByPsiFiles(commonKtFiles)
            sourceScope -= commonSourcesScope
            ktFiles = ktFiles.filterNot { it.isCommonSource == true }
            createSession(
                "${module.getModuleName()}-common",
                CommonPlatforms.defaultCommonPlatform,
                CommonPlatformAnalyzerServices,
                commonSourcesScope
            )
        }

        val session = createSession(
            module.getModuleName(),
            JvmPlatforms.unspecifiedJvmPlatform,
            JvmPlatformAnalyzerServices,
            sourceScope
        ) {
            if (commonSession != null) {
                sourceDependsOnDependencies(listOf(commonSession.moduleData))
            }
            friendDependencies(module.getFriendPaths())
        }

        val commonRawFir = commonSession?.buildFirFromKtFiles(commonKtFiles)
        val rawFir = session.buildFirFromKtFiles(ktFiles)

        commonSession?.apply {
            val (commonScopeSession, commonFir) = runResolution(commonRawFir!!)
            runCheckers(commonScopeSession, commonFir, diagnosticsReporter)
        }

        val (scopeSession, fir) = session.runResolution(rawFir)
        session.runCheckers(scopeSession, fir, diagnosticsReporter)

        return if (syntaxErrors || diagnosticsReporter.hasErrors) null else FirResult(session, scopeSession, fir)
    }

    private fun CompilationContext.createComponentsForIncrementalCompilation(
        sourceScope: AbstractProjectFileSearchScope
    ): FirSessionFactory.ProviderAndScopeForIncrementalCompilation? {
        if (targetIds == null || incrementalComponents == null) return null
        val directoryWithIncrementalPartsFromPreviousCompilation =
            moduleConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
                ?: return null
        val incrementalCompilationScope = directoryWithIncrementalPartsFromPreviousCompilation.walk()
            .filter { it.extension == "class" }
            .let { projectEnvironment.getSearchScopeByIoFiles(it.asIterable()) }
            .takeIf { !it.isEmpty }
            ?: return null
        val packagePartProvider = IncrementalPackagePartProvider(
            projectEnvironment.getPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        )
        return FirSessionFactory.ProviderAndScopeForIncrementalCompilation(packagePartProvider, incrementalCompilationScope)
    }

    private fun CompilationContext.runBackend(
        ktFiles: List<KtFile>,
        fir2IrResult: Fir2IrResult,
        extensions: JvmGeneratorExtensionsImpl,
        session: FirSession,
        diagnosticsReporter: BaseDiagnosticsCollector
    ): GenerationState {
        val (moduleFragment, symbolTable, components) = fir2IrResult
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext
        val codegenFactory = JvmIrCodegenFactory(
            moduleConfiguration,
            moduleConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG),
            jvmGeneratorExtensions = extensions
        )

        val generationState = GenerationState.Builder(
            (projectEnvironment as PsiBasedProjectEnvironment).project, ClassBuilderFactories.BINARIES,
            moduleFragment.descriptor, dummyBindingContext, ktFiles,
            moduleConfiguration
        ).codegenFactory(
            codegenFactory
        ).withModule(
            module
        ).onIndependentPartCompilationEnd(
            createOutputFilesFlushingCallbackIfPossible(moduleConfiguration)
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).diagnosticReporter(
            diagnosticsReporter
        ).build()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        performanceManager?.notifyIRLoweringStarted()
        generationState.beforeCompile()
        codegenFactory.generateModuleInFrontendIRMode(
            generationState, moduleFragment, symbolTable, extensions, FirJvmBackendExtension(session, components)
        ) {
            performanceManager?.notifyIRLoweringFinished()
            performanceManager?.notifyIRGenerationStarted()
        }
        CodegenFactory.doCheckCancelled(generationState)
        generationState.factory.done()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        AnalyzerWithCompilerReport.reportDiagnostics(
            FilteredJvmDiagnostics(
                generationState.collectedExtraJvmDiagnostics,
                dummyBindingContext.diagnostics
            ),
            messageCollector,
            renderDiagnosticName
        )
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return generationState
    }

    private class CompilationContext(
        val module: Module,
        val allSources: List<KtFile>,
        val projectEnvironment: AbstractProjectEnvironment,
        val messageCollector: MessageCollector,
        val renderDiagnosticName: Boolean,
        val moduleConfiguration: CompilerConfiguration,
        val performanceManager: CommonCompilerPerformanceManager?,
        val targetIds: List<TargetId>?,
        val incrementalComponents: IncrementalCompilationComponents?,
        val extendedAnalysisMode: Boolean,
        val firExtensionRegistrars: List<FirExtensionRegistrar>,
        val irGenerationExtensions: Collection<IrGenerationExtension>
    )

    private fun findMainClass(firResult: FirResult): FqName? {
        // TODO: replace with proper main function detector, KT-44557
        val compatibleClasses = mutableListOf<FqName>()
        val visitor = object : FirVisitorVoid() {
            lateinit var file: FirFile

            override fun visitElement(element: FirElement) {}

            override fun visitFile(file: FirFile) {
                this.file = file
                file.acceptChildren(this)
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                if (simpleFunction.name.asString() != "main") return
                if (simpleFunction.typeParameters.isNotEmpty()) return
                when (simpleFunction.valueParameters.size) {
                    0 -> {}
                    1 -> {
                        val parameterType = simpleFunction.valueParameters.single().returnTypeRef.coneType
                        if (!parameterType.isArrayType || parameterType.arrayElementType()?.isString != true) return
                    }
                    else -> return
                }

                compatibleClasses += FqName.fromSegments(
                    file.packageFqName.pathSegments().map { it.asString() } + "${file.name.removeSuffix(".kt").capitalize()}Kt"
                )
            }
        }
        firResult.fir.forEach { it.accept(visitor) }
        return compatibleClasses.singleOrNull()
    }
}
