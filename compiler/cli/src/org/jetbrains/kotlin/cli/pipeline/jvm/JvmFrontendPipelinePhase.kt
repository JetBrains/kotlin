/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.DefaultDiagnosticReporter
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.targetDescription
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.FirJvmIncrementalCompilationSymbolProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.createSymbolProviders
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.firCachesFactoryForCliMode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File

object JvmFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JvmFrontendPipelineArtifact? {
        val (configuration, diagnosticsCollector, rootDisposable) = input
        val messageCollector = configuration.messageCollector

        if (!checkNotSupportedPlugins(configuration, messageCollector)) {
            return null
        }

        val chunk = configuration.moduleChunk!!
        val targetDescription = chunk.targetDescription()
        val (environment, sourcesProvider) = createEnvironmentAndSources(
            configuration,
            rootDisposable,
            targetDescription,
            diagnosticsCollector
        ) ?: return null

        FirAnalysisHandlerExtension.analyze(environment.project, configuration)?.let {
            /*
             * If the analysis handler exception finishes successfully, we should stop the pipeline (as it doesn't produce the proper
             * fronted artifact), but we don't need to return the [ExitCode.COMPILATION_ERROR] (because the "compilation" finished
             * successfully). Ideally, it should be implemented in a way, when analysis handler extensions are run in the dedicated
             * pipeline (TODO: KT-73576), so this is a temporary solution.
             */
            when (it) {
                true -> throw SuccessfulPipelineExecutionException()
                false -> throw PipelineStepException(definitelyCompilationError = true)
            }
        }

        val performanceManager = configuration.perfManager
        performanceManager?.notifyCompilerInitialized(files = 0, lines = 0, targetDescription)

        val sources = sourcesProvider()
        val allSources = sources.allFiles

        if (
            allSources.isEmpty() &&
            !configuration.allowNoSourceFiles &&
            configuration.buildFile == null
        ) {
            if (!configuration.printVersion) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "No source files")
            }
            return null
        }

        performanceManager?.notifyAnalysisStarted()
        val sourceScope: AbstractProjectFileSearchScope
        when (configuration.useLightTree) {
            true -> {
                sourceScope = AbstractProjectFileSearchScope.EMPTY
            }
            false -> {
                val ktFiles = allSources.map { (it as KtPsiSourceFile).psiFile as KtFile }
                sourceScope = environment.getSearchScopeByPsiFiles(ktFiles) + environment.getSearchScopeForProjectJavaSources()
                if (checkIfScriptsInCommonSources(configuration, ktFiles)) {
                    return null
                }
            }
        }

        var librariesScope = environment.getSearchScopeForProjectLibraries()
        val incrementalCompilationScope = createIncrementalCompilationScope(
            configuration,
            environment,
            incrementalExcludesScope = sourceScope
        )?.also { librariesScope -= it }

        val moduleName = when {
            chunk.modules.size > 1 -> chunk.modules.joinToString(separator = "+") { it.getModuleName() }
            else -> configuration.moduleName!!
        }

        val libraryList = createLibraryListForJvm(
            moduleName,
            configuration,
            friendPaths = chunk.modules.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() }
        )

        val sessionsWithSources = prepareJvmSessions<KtSourceFile>(
            files = allSources,
            rootModuleName = Name.special("<$moduleName>"),
            configuration = configuration,
            projectEnvironment = environment,
            librariesScope = librariesScope,
            libraryList = libraryList,
            isCommonSource = sources.isCommonSourceForLt,
            isScript = { ((it as? KtPsiSourceFile)?.psiFile as? KtFile)?.isScript() == true },
            fileBelongsToModule = sources.fileBelongsToModuleForLt,
            createProviderAndScopeForIncrementalCompilation = { files ->
                val scope = environment.getSearchScopeBySourceFiles(files)
                createContextForIncrementalCompilation(
                    configuration,
                    environment,
                    scope,
                    previousStepsSymbolProviders = emptyList(),
                    incrementalCompilationScope
                )
            }
        )

        val countFilesAndLines = if (performanceManager == null) null else performanceManager::addSourcesStats
        val outputs = sessionsWithSources.map { (session, sources) ->
            val rawFirFiles = when (configuration.useLightTree) {
                true -> session.buildFirViaLightTree(sources, diagnosticsCollector, countFilesAndLines)
                else -> session.buildFirFromKtFiles(sources.asKtFilesList())
            }
            resolveAndCheckFir(session, rawFirFiles, diagnosticsCollector)
        }
        outputs.runPlatformCheckers(diagnosticsCollector)

        val kotlinPackageUsageIsFine = when (configuration.useLightTree) {
            true -> outputs.all { checkKotlinPackageUsageForLightTree(configuration, it.fir) }
            false -> sessionsWithSources.all { (_, sources) -> checkKotlinPackageUsageForPsi(configuration, sources.asKtFilesList()) }
        }

        if (!kotlinPackageUsageIsFine) return null

        val firResult = FirResult(outputs)
        return JvmFrontendPipelineArtifact(firResult, configuration, environment, diagnosticsCollector, allSources)
    }

    private data class EnvironmentAndSources(val environment: VfsBasedProjectEnvironment, val sources: () -> GroupedKtSources)

    /**
     * Calculation of sources should be postponed due to analysis handler extensions.
     * To call the extensions, we need to have an instance of project, and the extension might suppress any errors
     * caused by sources parsing.
     *
     * Also, since it's necessary to have the instance of KotlinCoreEnvironment to build the KtFiles, which
     * we don't want to leak outside of [createEnvironmentAndSources] method, it's not possible to split this method into twos (one
     * for building environment, one for building sources)
     */
    private fun createEnvironmentAndSources(
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        targetDescription: String,
        diagnosticReporter: BaseDiagnosticsCollector
    ): EnvironmentAndSources? {
        val messageCollector = configuration.messageCollector
        return when (configuration.useLightTree) {
            true -> {
                val environment = createProjectEnvironment(
                    configuration,
                    rootDisposable,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES,
                    messageCollector
                )
                val sources = { collectSources(configuration, environment.project, messageCollector) }
                EnvironmentAndSources(environment, sources)
            }
            false -> {
                val kotlinCoreEnvironment = K2JVMCompiler.Companion.createCoreEnvironment(
                    rootDisposable, configuration, messageCollector,
                    targetDescription
                ) ?: return null

                val projectEnvironment = VfsBasedProjectEnvironment(
                    kotlinCoreEnvironment.project,
                    VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
                ) { kotlinCoreEnvironment.createPackagePartProvider(it) }

                val sources = {
                    val ktFiles = kotlinCoreEnvironment.getSourceFiles()
                    ktFiles.forEach { AnalyzerWithCompilerReport.reportSyntaxErrors(it, diagnosticReporter) }
                    groupKtFiles(ktFiles)
                }

                EnvironmentAndSources(projectEnvironment, sources)
            }
        }.takeUnless { messageCollector.hasErrors() }
    }

    private fun groupKtFiles(ktFiles: List<KtFile>): GroupedKtSources {
        val platformSources = mutableSetOf<KtPsiSourceFile>()
        val commonSources = mutableSetOf<KtPsiSourceFile>()
        val sourcesByModuleName = mutableMapOf<String, MutableSet<KtPsiSourceFile>>()

        for (ktFile in ktFiles) {
            val sourceFile = KtPsiSourceFile(ktFile)
            when (val moduleName = ktFile.hmppModuleName) {
                null -> when {
                    ktFile.isCommonSource == true -> commonSources.add(sourceFile)
                    else -> platformSources.add(sourceFile)
                }
                else -> {
                    commonSources.add(sourceFile)
                    sourcesByModuleName.getOrPut(moduleName) { mutableSetOf() }.add(sourceFile)
                }
            }
        }
        return GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
    }

    private fun checkNotSupportedPlugins(
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
    ): Boolean {
        val notSupportedPlugins = mutableListOf<String?>().apply {
            compilerConfiguration.get(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, ComponentRegistrar::supportsK2)
            compilerConfiguration.get(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, CompilerPluginRegistrar::supportsK2)
        }

        if (notSupportedPlugins.isNotEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                """
                    |There are some plugins incompatible with language version 2.0:
                    |${notSupportedPlugins.joinToString(separator = "\n|") { "  $it" }}
                    |Please use language version 1.9 or below
                """.trimMargin()
            )
            return false
        }

        return true
    }

    private fun <T : Any> List<T>?.collectIncompatiblePluginNamesTo(
        destination: MutableList<String?>,
        supportsK2: T.() -> Boolean
    ) {
        this?.filter { !it.supportsK2() && it::class.java.canonicalName != CLICompiler.SCRIPT_PLUGIN_REGISTRAR_NAME }
            ?.mapTo(destination) { it::class.qualifiedName }
    }

    fun checkIfScriptsInCommonSources(configuration: CompilerConfiguration, ktFiles: List<KtFile>): Boolean {
        val lastHmppModule = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)?.modules?.lastOrNull()
        val commonScripts = ktFiles.filter { it.isScript() && (it.isCommonSource == true || it.hmppModuleName != lastHmppModule?.name) }
        if (commonScripts.isNotEmpty()) {
            val cwd = File(".").absoluteFile
            fun renderFile(ktFile: KtFile) = File(ktFile.virtualFilePath).descendantRelativeTo(cwd).path
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Script files in common source roots are not supported. Misplaced files:\n    " +
                        commonScripts.joinToString("\n    ", transform = ::renderFile)
            )
            return true
        }
        return false
    }

    fun <F> prepareJvmSessions(
        files: List<F>,
        rootModuleName: Name,
        configuration: CompilerConfiguration,
        projectEnvironment: VfsBasedProjectEnvironment,
        librariesScope: AbstractProjectFileSearchScope,
        libraryList: DependencyListForCliModule,
        isCommonSource: (F) -> Boolean,
        isScript: (F) -> Boolean,
        fileBelongsToModule: (F, String) -> Boolean,
        createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
    ): List<SessionWithSources<F>> {
        val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
        val javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources()
        val predefinedJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)

        var firJvmIncrementalCompilationSymbolProviders: FirJvmIncrementalCompilationSymbolProviders? = null
        var firJvmIncrementalCompilationSymbolProvidersIsInitialized = false

        return SessionConstructionUtils.prepareSessions(
            files, configuration, rootModuleName, JvmPlatforms.unspecifiedJvmPlatform,
            metadataCompilationMode = false, libraryList, isCommonSource, isScript, fileBelongsToModule,
            createLibrarySession = { sessionProvider ->
                FirJvmSessionFactory.createLibrarySession(
                    rootModuleName,
                    sessionProvider,
                    libraryList.moduleDataProvider,
                    projectEnvironment,
                    extensionRegistrars,
                    librariesScope,
                    projectEnvironment.getPackagePartProvider(librariesScope),
                    configuration.languageVersionSettings,
                    predefinedJavaComponents = predefinedJavaComponents,
                )
            },
        ) { moduleFiles, moduleData, sessionProvider, sessionConfigurator ->
            FirJvmSessionFactory.createModuleBasedSession(
                moduleData,
                sessionProvider,
                javaSourcesScope,
                projectEnvironment,
                createIncrementalCompilationSymbolProviders = { session ->
                    // Temporary solution for KT-61942 - we need to share the provider built on top of previously compiled files,
                    // because we do not distinguish classes generated from common and platform sources, so may end up with the
                    // same type loaded from both. And if providers are not shared, the types will not match on the actualizing.
                    // The proper solution would be to build IC providers only on class files generated for the currently compiled module.
                    // But to solve it we need to have a mapping from module to its class files.
                    // TODO: reimplement with splitted providers after fixing KT-62686
                    if (firJvmIncrementalCompilationSymbolProvidersIsInitialized) firJvmIncrementalCompilationSymbolProviders
                    else {
                        firJvmIncrementalCompilationSymbolProvidersIsInitialized = true
                        createProviderAndScopeForIncrementalCompilation(moduleFiles)
                            ?.createSymbolProviders(session, moduleData, projectEnvironment)?.also {
                                firJvmIncrementalCompilationSymbolProviders = it
                            }
                    }
                },
                extensionRegistrars,
                configuration.languageVersionSettings,
                configuration.get(JVMConfigurationKeys.JVM_TARGET, JvmTarget.DEFAULT),
                configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
                configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER),
                configuration.get(CommonConfigurationKeys.IMPORT_TRACKER),
                predefinedJavaComponents = predefinedJavaComponents,
                needRegisterJavaElementFinder = true,
                sessionConfigurator,
            )
        }
    }
}
