/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_PLUGIN_INITIALIZATION_ERROR
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.CONTENT_ROOTS
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.targetDescription
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase.createEnvironmentAndSources
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

object JvmFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JvmFrontendPipelineArtifact? {
        val (configuration, rootDisposable) = input
        val diagnosticsCollector = configuration.diagnosticsCollector

        val perfManager = configuration.perfManager
        val chunk = configuration.moduleChunk!!
        val targetDescription = chunk.targetDescription()
        perfManager?.targetDescription = targetDescription

        if (!checkNotSupportedPlugins(configuration)) {
            perfManager?.notifyPhaseFinished(PhaseType.Initialization)
            return null
        }

        val (environment, sourcesProvider) = createEnvironmentAndSources(
            configuration,
            rootDisposable,
            targetDescription,
            diagnosticsCollector
        ) ?: run {
            perfManager?.notifyPhaseFinished(PhaseType.Initialization)
            return null
        }

        runAnalysisHandlerExtensions(environment.project, configuration)?.let {
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

        val sources = sourcesProvider()
        val allSources = sources.allFiles

        perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        if (
            allSources.isEmpty() &&
            !configuration.allowNoSourceFiles &&
            configuration.buildFile == null
        ) {
            if (!configuration.printVersion) {
                configuration.report(COMPILER_ARGUMENTS_ERROR, "No source files")
            }
            return null
        }

        perfManager?.notifyPhaseStarted(PhaseType.Analysis)
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

        val sessionsWithSources = prepareJvmSessions(
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

        val countFilesAndLines = if (perfManager == null) null else perfManager::addSourcesStats
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

        val frontendOutput = AllModulesFrontendOutput(outputs)
        return JvmFrontendPipelineArtifact(frontendOutput, configuration, environment, allSources)
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
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                )
                val sources = { collectSources(configuration, environment) }
                EnvironmentAndSources(environment, sources)
            }
            false -> {
                val kotlinCoreEnvironment = K2JVMCompiler.createCoreEnvironment(
                    rootDisposable, configuration, messageCollector,
                    targetDescription
                ) ?: return null

                val projectEnvironment = kotlinCoreEnvironment.toVfsBasedProjectEnvironment()

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

    private fun checkNotSupportedPlugins(compilerConfiguration: CompilerConfiguration): Boolean {
        val notSupportedPlugins = mutableListOf<String?>().apply {
            compilerConfiguration.get(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, ComponentRegistrar::supportsK2)
            compilerConfiguration.get(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, CompilerPluginRegistrar::supportsK2)
        }

        if (notSupportedPlugins.isNotEmpty()) {
            compilerConfiguration.report(
                COMPILER_PLUGIN_INITIALIZATION_ERROR,
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

    private fun checkIfScriptsInCommonSources(configuration: CompilerConfiguration, ktFiles: List<KtFile>): Boolean {
        val lastHmppModule = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)?.modules?.lastOrNull()
        val commonScripts = ktFiles.filter { it.isScript() && (it.isCommonSource == true || it.hmppModuleName != lastHmppModule?.name) }
        if (commonScripts.isNotEmpty()) {
            val cwd = File(".").absoluteFile
            fun renderFile(ktFile: KtFile) = File(ktFile.virtualFilePath).descendantRelativeTo(cwd).path
            configuration.report(
                COMPILER_ARGUMENTS_ERROR,
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
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources()

        var firJvmIncrementalCompilationSymbolProviders: FirJvmIncrementalCompilationSymbolProviders? = null
        var firJvmIncrementalCompilationSymbolProvidersIsInitialized = false
        val context = FirJvmSessionFactory.Context(
            configuration,
            projectEnvironment,
            librariesScope,
        )

        return SessionConstructionUtils.prepareSessions(
            files, configuration, rootModuleName, JvmPlatforms.unspecifiedJvmPlatform,
            metadataCompilationMode = false, libraryList, extensionRegistrars, isCommonSource, isScript, fileBelongsToModule,
            createMetadataSessionFactoryContextForHmppCommonLibrarySession = {
                AbstractFirMetadataSessionFactory.Context(
                    createJvmContext = { context },
                    createJsContext = { shouldNotBeCalled() }
                )
            },
            createSharedLibrarySession = {
                FirJvmSessionFactory.createSharedLibrarySession(
                    rootModuleName,
                    extensionRegistrars,
                    configuration.languageVersionSettings,
                    context,
                )
            },
            createLibrarySession = { sharedLibrarySession ->
                FirJvmSessionFactory.createLibrarySession(
                    sharedLibrarySession,
                    libraryList.moduleDataProvider,
                    extensionRegistrars,
                    configuration.languageVersionSettings,
                    context,
                )
            },
            createSourceSession = { moduleFiles, moduleData, isForLeafHmppModule, sessionConfigurator ->
                FirJvmSessionFactory.createSourceSession(
                    moduleData,
                    javaSourcesScope,
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
                    configuration,
                    context,
                    needRegisterJavaElementFinder = true,
                    isForLeafHmppModule = isForLeafHmppModule,
                    sessionConfigurator,
                )
            }
        )
    }

    fun dumpModel(
        dir: String,
        chunk: List<Module>,
        configuration: CompilerConfiguration,
        arguments: CommonCompilerArguments,
    ) {
        val dirFile = File(dir)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        val fileName = "model-${chunk.first().getModuleName()}"
        var counter = 0
        fun file(): File {
            val postfix = if (counter != 0) ".$counter" else ""
            return File(dirFile, "$fileName$postfix.xml")
        }

        var outputFile: File
        do {
            outputFile = file()
            counter++
        } while (outputFile.exists())

        // Write XML using StAX
        outputFile.bufferedWriter().use { writer ->
            val xmlFactory = XMLOutputFactory.newInstance()
            with(xmlFactory.createXMLStreamWriter(writer)) {
                writeStartDocument("UTF-8", "1.0")
                val depth = PrettyPrintDepth(0)

                // <modules>
                start("modules", depth)

                // compilerArguments
                start("compilerArguments", depth)
                for (arg in ArgumentUtils.convertArgumentsToStringList(arguments)) {
                    empty("arg", depth)
                    writeAttribute("value", arg)
                }
                end(depth) // compilerArguments

                // modules
                for (module in chunk) {
                    start("module", depth)
                    writeAttribute("timestamp", System.currentTimeMillis().toString())
                    writeAttribute("name", module.getModuleName())
                    writeAttribute("type", module.getModuleType())
                    writeAttribute("outputDir", module.getOutputDirectory())

                    for (friendDir in module.getFriendPaths()) {
                        empty("friendDir", depth)
                        writeAttribute("path", friendDir)
                    }
                    for (source in module.getSourceFiles()) {
                        empty("sources", depth)
                        writeAttribute("path", source)
                    }
                    for (javaSourceRoots in module.getJavaSourceRoots()) {
                        start("javaSourceRoots", depth)
                        writeAttribute("path", javaSourceRoots.path)
                        javaSourceRoots.packagePrefix?.let { writeAttribute("packagePrefix", it) }
                        end(depth)
                    }
                    for (classpath in configuration.get(CONTENT_ROOTS).orEmpty()) {
                        when (classpath) {
                            is JvmClasspathRoot -> {
                                empty("classpath", depth)
                                writeAttribute("path", classpath.file.absolutePath)
                            }
                            is JvmModulePathRoot -> {
                                empty("modulepath", depth)
                                writeAttribute("path", classpath.file.absolutePath)
                            }
                        }
                    }
                    for (commonSources in module.getCommonSourceFiles()) {
                        empty("commonSources", depth)
                        writeAttribute("path", commonSources)
                    }
                    module.modularJdkRoot?.let {
                        empty("modularJdkRoot", depth)
                        writeAttribute("path", it)
                    }

                    end(depth) // module
                }

                end(depth) // modules
                writeCharacters("\n")
                writeEndDocument()
                flush()
                close()
            }
        }
    }

    /**
     * Applies [FirAnalysisHandlerExtension] instances to a project
     * @param project the project to analyze
     * @param configuration compiler configuration
     * @return [null] if no applicable extensions were found, [true] if all applicable extensions returned [true] from [doAnalysis],
     * [false] if any applicable extension returned [false]
     *
     * @see FirAnalysisHandlerExtension.isApplicable
     * @see FirAnalysisHandlerExtension.doAnalysis
     */

    fun runAnalysisHandlerExtensions(project: Project, configuration: CompilerConfiguration): Boolean? {
        val extensions = configuration.getCompilerExtensions(FirAnalysisHandlerExtension)
            .filter { it.isApplicable(configuration) }
            .takeIf { it.isNotEmpty() }
            ?: return null
        return extensions.all { it.doAnalysis(project, configuration) }
    }
}

// Pretty-printing helpers for StAX writer
private data class PrettyPrintDepth(var value: Int)

private fun XMLStreamWriter.indent(depth: PrettyPrintDepth) {
    writeCharacters("\n")
    if (depth.value > 0) writeCharacters("  ".repeat(depth.value))
}

private fun XMLStreamWriter.start(name: String, depth: PrettyPrintDepth) {
    indent(depth)
    writeStartElement(name)
    depth.value++
}

private fun XMLStreamWriter.end(depth: PrettyPrintDepth) {
    depth.value--
    indent(depth)
    writeEndElement()
}

private fun XMLStreamWriter.empty(name: String, depth: PrettyPrintDepth) {
    indent(depth)
    writeEmptyElement(name)
}
