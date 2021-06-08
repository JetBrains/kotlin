/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionFactory.createSessionWithDependencies
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.io.File

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

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val projectConfiguration = environment.configuration
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val outputs = newLinkedHashMapWithExpectedSize<Module, GenerationState>(chunk.size)
        val targetIds = environment.configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
        val incrementalComponents = environment.configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        for (module in chunk) {
            performanceManager?.notifyAnalysisStarted()
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = module.getSourceFiles(environment, localFileSystem, chunk.size > 1, buildFile)
            if (!checkKotlinPackageUsage(environment, ktFiles)) return false

            val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
                AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, environment.messageCollector).isHasErrors or errorsFound
            }

            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)

            val sourceScope = GlobalSearchScope.filesWithoutLibrariesScope(project, ktFiles.map { it.virtualFile })
                .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))

            var librariesScope = ProjectScope.getLibrariesScope(project)

            val providerAndScopeForIncrementalCompilation = run {
                if (targetIds == null || incrementalComponents == null) return@run null
                val fileSystem = environment.projectEnvironment.environment.localFileSystem
                val directoryWithIncrementalPartsFromPreviousCompilation =
                    moduleConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
                        ?: return@run null
                val previouslyCompiledFiles = directoryWithIncrementalPartsFromPreviousCompilation.walk()
                    .filter { it.extension == "class" }
                    .mapNotNull { fileSystem.findFileByIoFile(it) }
                    .toList()
                    .takeIf { it.isNotEmpty() }
                    ?: return@run null
                val packagePartProvider = IncrementalPackagePartProvider(
                    environment.createPackagePartProvider(sourceScope),
                    targetIds.map(incrementalComponents::getIncrementalCache)
                )
                val incrementalCompilationScope = GlobalSearchScope.filesWithoutLibrariesScope(
                    project,
                    previouslyCompiledFiles
                )
                librariesScope = librariesScope.intersectWith(GlobalSearchScope.notScope(incrementalCompilationScope))
                FirSessionFactory.ProviderAndScopeForIncrementalCompilation(packagePartProvider, incrementalCompilationScope)
            }

            val languageVersionSettings = moduleConfiguration.languageVersionSettings
            val session = createSessionWithDependencies(
                Name.identifier(module.getModuleName()),
                JvmPlatforms.unspecifiedJvmPlatform,
                JvmPlatformAnalyzerServices,
                externalSessionProvider = null,
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
                },
                sessionConfigurator = {
                    if (extendedAnalysisMode) {
                        registerExtendedCommonCheckers()
                    }
                }
            )

            val firAnalyzerFacade = FirAnalyzerFacade(session, languageVersionSettings, ktFiles)

            firAnalyzerFacade.runResolution()
            val firDiagnostics = firAnalyzerFacade.runCheckers().values.flatten()
            val hasErrors = FirDiagnosticsCompilerResultsReporter.reportDiagnostics(firDiagnostics, environment.messageCollector)
            performanceManager?.notifyAnalysisFinished()

            if (syntaxErrors || hasErrors) {
                return false
            }

            performanceManager?.notifyGenerationStarted()

            performanceManager?.notifyIRTranslationStarted()
            val extensions = JvmGeneratorExtensionsImpl()
            val (moduleFragment, symbolTable, components) = firAnalyzerFacade.convertToIr(extensions)

            performanceManager?.notifyIRTranslationFinished()

            val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

            val codegenFactory = JvmIrCodegenFactory(moduleConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases))

            // Create and initialize the module and its dependencies
            val container = TopDownAnalyzerFacadeForJVM.createContainer(
                project, ktFiles, NoScopeRecordCliBindingTrace(), environment.configuration, environment::createPackagePartProvider,
                ::FileBasedDeclarationProviderFactory, CompilerEnvironment,
                TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles), emptyList()
            )

            val generationState = GenerationState.Builder(
                environment.project, ClassBuilderFactories.BINARIES,
                container.get(), dummyBindingContext, ktFiles,
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

            performanceManager?.notifyIRGenerationFinished()
            performanceManager?.notifyGenerationFinished()
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            outputs[module] = generationState

            PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
            Disposer.dispose(environment.project)
        }

        val mainClassFqName: FqName? =
            if (chunk.size == 1 && projectConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null)
                TODO(".jar output is not yet supported for -Xuse-fir: KT-42868")
            else null

        return writeOutputs(environment, projectConfiguration, chunk, outputs, mainClassFqName)
    }
}
