/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionFactory.createSessionWithDependencies
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
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.io.File
import kotlin.collections.set

object FirKotlinToJvmBytecodeCompiler {
    fun compileModulesUsingFrontendIR(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
        extendedAnalysisMode: Boolean
    ): Boolean {
        val project = environment.project
        val performanceManager = environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)

        environment.messageCollector.report(
            STRONG_WARNING,
            "ATTENTION!\n This build uses in-dev FIR: \n  -Xuse-fir"
        )


        val psiFinderExtensionPoint = PsiElementFinder.EP.getPoint(project)
        if (psiFinderExtensionPoint.extensionList.any { it is JavaElementFinder }) {
            psiFinderExtensionPoint.unregisterExtension(JavaElementFinder::class.java)
        }

        val projectConfiguration = environment.configuration
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val outputs = newLinkedHashMapWithExpectedSize<Module, GenerationState>(chunk.size)
        val targetIds = environment.configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
        val incrementalComponents = environment.configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        val isMultiModuleChunk = chunk.size > 1

        for (module in chunk) {
            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)
            val context = CompilationContext(
                module,
                project,
                environment,
                moduleConfiguration,
                localFileSystem,
                isMultiModuleChunk,
                buildFile,
                performanceManager,
                targetIds,
                incrementalComponents,
                extendedAnalysisMode
            )
            val generationState = context.compileModule() ?: return false
            outputs[module] = generationState

            Disposer.dispose(environment.project)
        }

        val mainClassFqName: FqName? =
            if (chunk.size == 1 && projectConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null)
                TODO(".jar output is not yet supported for -Xuse-fir: KT-42868")
            else null

        return writeOutputs(environment, projectConfiguration, chunk, outputs, mainClassFqName)
    }

    private fun CompilationContext.compileModule(): GenerationState? {
        performanceManager?.notifyAnalysisStarted()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val ktFiles = module.getSourceFiles(environment, localFileSystem, isMultiModuleChunk, buildFile)
        if (!checkKotlinPackageUsage(environment, ktFiles)) return null

        val firAnalyzerFacade = runFrontend(ktFiles).also {
            performanceManager?.notifyAnalysisFinished()
        } ?: return null

        performanceManager?.notifyGenerationStarted()
        performanceManager?.notifyIRTranslationStarted()

        val extensions = JvmGeneratorExtensionsImpl(moduleConfiguration)
        val fir2IrResult = firAnalyzerFacade.convertToIr(extensions)
        val session = firAnalyzerFacade.session

        performanceManager?.notifyIRTranslationFinished()

        val generationState = runBackend(
            ktFiles,
            fir2IrResult,
            extensions,
            session
        )

        performanceManager?.notifyIRGenerationFinished()
        performanceManager?.notifyGenerationFinished()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return generationState
    }

    private fun CompilationContext.runFrontend(ktFiles: List<KtFile>): FirAnalyzerFacade? {
        @Suppress("NAME_SHADOWING")
        var ktFiles = ktFiles
        val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
            AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, environment.messageCollector).isHasErrors or errorsFound
        }

        var sourceScope = GlobalSearchScope.filesWithoutLibrariesScope(project, ktFiles.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))

        var librariesScope = ProjectScope.getLibrariesScope(project)

        val providerAndScopeForIncrementalCompilation = createComponentsForIncrementalCompilation(sourceScope)

        providerAndScopeForIncrementalCompilation?.scope?.let {
            librariesScope = librariesScope.intersectWith(GlobalSearchScope.notScope(it))
        }

        val languageVersionSettings = moduleConfiguration.languageVersionSettings

        val commonKtFiles = ktFiles.filter { it.isCommonSource == true }

        val sessionProvider = FirProjectSessionProvider()

        fun createSession(
            name: String,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices,
            sourceScope: GlobalSearchScope,
            dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {}
        ): FirSession {
            return createSessionWithDependencies(
                Name.identifier(name),
                platform,
                analyzerServices,
                externalSessionProvider = sessionProvider,
                project,
                languageVersionSettings,
                sourceScope,
                librariesScope,
                lookupTracker = environment.configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
                providerAndScopeForIncrementalCompilation,
                getPackagePartProvider = { environment.createPackagePartProvider(it) },
                dependenciesConfigurator = {
                    dependencies(moduleConfiguration.jvmClasspathRoots.map { it.toPath() })
                    dependencies(moduleConfiguration.jvmModularRoots.map { it.toPath() })
                    friendDependencies(moduleConfiguration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                    dependenciesConfigurator()
                },
                sessionConfigurator = {
                    if (extendedAnalysisMode) {
                        registerExtendedCommonCheckers()
                    }
                }
            )
        }

        val commonSession = runIf(
            languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) && commonKtFiles.isNotEmpty()
        ) {
            val commonSourcesScope = GlobalSearchScope.filesWithoutLibrariesScope(project, commonKtFiles.map { it.virtualFile })
            sourceScope = sourceScope.intersectWith(GlobalSearchScope.notScope(commonSourcesScope))
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

        val commonAnalyzerFacade = commonSession?.let { FirAnalyzerFacade(it, languageVersionSettings, commonKtFiles) }
        val firAnalyzerFacade = FirAnalyzerFacade(session, languageVersionSettings, ktFiles)

        commonAnalyzerFacade?.runResolution()
        val allFirDiagnostics = mutableListOf<FirDiagnostic>()
        commonAnalyzerFacade?.runCheckers()?.values?.flattenTo(allFirDiagnostics)
        firAnalyzerFacade.runResolution()
        firAnalyzerFacade.runCheckers().values.flattenTo(allFirDiagnostics)
        val hasErrors = FirDiagnosticsCompilerResultsReporter.reportDiagnostics(allFirDiagnostics, environment.messageCollector)

        return firAnalyzerFacade.takeUnless { syntaxErrors || hasErrors }
    }

    private fun CompilationContext.createComponentsForIncrementalCompilation(
        sourceScope: GlobalSearchScope
    ): FirSessionFactory.ProviderAndScopeForIncrementalCompilation? {
        if (targetIds == null || incrementalComponents == null) return null
        val fileSystem = environment.projectEnvironment.environment.localFileSystem
        val directoryWithIncrementalPartsFromPreviousCompilation =
            moduleConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
                ?: return null
        val previouslyCompiledFiles = directoryWithIncrementalPartsFromPreviousCompilation.walk()
            .filter { it.extension == "class" }
            .mapNotNull { fileSystem.findFileByIoFile(it) }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: return null
        val packagePartProvider = IncrementalPackagePartProvider(
            environment.createPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        )
        val incrementalCompilationScope = GlobalSearchScope.filesWithoutLibrariesScope(
            project,
            previouslyCompiledFiles
        )
        return FirSessionFactory.ProviderAndScopeForIncrementalCompilation(packagePartProvider, incrementalCompilationScope)
    }

    private fun CompilationContext.runBackend(
        ktFiles: List<KtFile>,
        fir2IrResult: Fir2IrResult,
        extensions: JvmGeneratorExtensionsImpl,
        session: FirSession
    ): GenerationState {
        val (moduleFragment, symbolTable, components) = fir2IrResult
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext
        val codegenFactory = JvmIrCodegenFactory(
            moduleConfiguration,
            moduleConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases),
            jvmGeneratorExtensions = extensions
        )

        val generationState = GenerationState.Builder(
            environment.project, ClassBuilderFactories.BINARIES,
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
            environment.messageCollector
        )
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return generationState
    }

    private class CompilationContext(
        val module: Module,
        val project: Project,
        val environment: KotlinCoreEnvironment,
        val moduleConfiguration: CompilerConfiguration,
        val localFileSystem: VirtualFileSystem,
        val isMultiModuleChunk: Boolean,
        val buildFile: File?,
        val performanceManager: CommonCompilerPerformanceManager?,
        val targetIds: List<TargetId>?,
        val incrementalComponents: IncrementalCompilationComponents?,
        val extendedAnalysisMode: Boolean
    )
}
