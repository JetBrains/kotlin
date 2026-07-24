/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_PLUGIN_INITIALIZATION_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.ROOTS_RESOLUTION_WARNING
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.configureProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase.createEnvironmentAndSources
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.java.direct.createJavaDirectSourceJavaFacadeBuilder
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.hmppModuleName
import org.jetbrains.kotlin.psi.isCommonSource
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import kotlin.io.path.Path

object JvmFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    @Suppress("IncorrectFormatting") // Destructuring declaration reformating is weird due to KTIJ-38744
    override fun executePhase(input: ConfigurationPipelineArtifact): JvmFrontendPipelineArtifact? {
        val (configuration, rootDisposable) = input

        val perfManager = configuration.perfManager
        val chunk = configuration.moduleChunk!!
        val targetDescription = chunk.targetDescription()
        perfManager?.targetDescription = targetDescription

        if (!checkNotSupportedPlugins(configuration)) {
            perfManager?.notifyPhaseFinished(PhaseType.Initialization)
            return null
        }

        (val environment, val sourcesProvider = sources) = createEnvironmentAndSources(
            configuration,
            rootDisposable,
            targetDescription,
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

        val [librariesScope, incrementalCompilationContext] = prepareIncrementalCompilationContextAndLibrariesScope(
            configuration,
            environment,
            incrementalExcludesScope = sourceScope
        )

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
            incrementalCompilationContext,
        )

        val countFilesAndLines = if (perfManager == null) null else perfManager::addSourcesStats
        val diagnosticsCollector = configuration.diagnosticsCollector
        val outputs = sessionsWithSources.map { (val session, val sources = files) ->
            val rawFirFiles = when (configuration.useLightTree) {
                true -> session.buildFirViaLightTree(sources, diagnosticsCollector, countFilesAndLines)
                else -> session.buildFirFromKtFiles(sources.asKtFilesList())
            }
            resolveAndCheckFir(session, rawFirFiles, diagnosticsCollector)
        }
        outputs.runPlatformCheckers(diagnosticsCollector)

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
    ): EnvironmentAndSources? {
        val diagnosticReporter = configuration.diagnosticsCollector
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
                val kotlinCoreEnvironment = createCoreEnvironment(
                    rootDisposable, configuration, targetDescription
                ) ?: return null

                val projectEnvironment = kotlinCoreEnvironment.toVfsBasedProjectEnvironment()

                val sources = {
                    val ktFiles = kotlinCoreEnvironment.getSourceFiles()
                    ktFiles.forEach { SyntaxErrorReporter.reportSyntaxErrors(it, diagnosticReporter) }
                    groupKtFiles(ktFiles)
                }

                EnvironmentAndSources(projectEnvironment, sources)
            }
        }.takeUnless { CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration) }
    }

    private fun groupKtFiles(ktFiles: List<KtFile>): GroupedKtSources {
        val platformSources = mutableSetOf<KtPsiSourceFile>()
        val commonSources = mutableSetOf<KtPsiSourceFile>()
        val sourcesByModuleName = mutableMapOf<String, MutableSet<KtPsiSourceFile>>()

        for (ktFile in ktFiles) {
            val sourceFile = KtPsiSourceFile(ktFile)
            @OptIn(KtImplementationDetail::class)
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
            compilerConfiguration[CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS]
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
        val lastHmppModule = configuration.hmppModuleStructure?.modules?.lastOrNull()

        @OptIn(KtImplementationDetail::class)
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

    fun createLibraryListForJvm(
        moduleName: String,
        configuration: CompilerConfiguration,
        friendPaths: List<String>
    ): DependencyListForCliModule {
        val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)

        val libraryList = DependencyListForCliModule.build(Name.identifier(moduleName)) {
            dependencies(
                contentRoots.mapNotNull {
                    when (it) {
                        is JvmClasspathRoot -> it.file.path
                        is VirtualJvmClasspathRoot if !it.isFriend -> it.file.toNioPath().toString()
                        else -> null
                    }
                }
            )
            friendDependencies(
                contentRoots
                    .filterIsInstance<VirtualJvmClasspathRoot>()
                    .filter { it.isFriend }
                    .map { it.file.toNioPath().toString() }
            )

            dependencies(configuration.jvmModularRoots.map { it.path })
            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            friendDependencies(friendPaths)
        }
        return libraryList
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
        incrementalCompilationContext: IncrementalCompilationContext?,
    ): List<SessionWithSources<F>> {
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources()

        /*
         * TODO(OSIP-75): This code is needed to preserve the legacy implementation of IC in KMP scenario, which was generally incorrect,
         *   but worked to some extent in some scenarios while the proper implementation is in development. It should be removed
         *   together with `if()` in the lambda below once we will get the full support of IC for KMP.
         */
        val isKmpCompilationWithLegacyIC = !configuration.useMetadataOnIncrementalClasspath
        var firJvmIncrementalCompilationSymbolProviders: FirJvmIncrementalCompilationSymbolProviders? = null
        var firJvmIncrementalCompilationSymbolProvidersIsInitialized = false

        val javaDirectFacade =
            if (configuration.useJavaDirect) {
                createJavaDirectSourceJavaFacadeBuilder(configuration, projectEnvironment)
            } else AbstractProjectEnvironment::getFirJavaFacade
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
            additionalProvidersForMetadataLibrarySessionsInHmppMode = l@{ session, moduleDataProvider, scopeProvider, libraries, rawRegularDependencies, rawFriendDependencies ->
                /*
                 * If no klibs were passed to the dependencies of the common fragment we try to interpret the fragment
                 * classpath as JVM classpath (with .jar and .class files).
                 *
                 * This could happen in a single-target project setup.
                 */
                if (libraries.isNotEmpty()) return@l emptyList()
                val dependencies = (rawRegularDependencies + rawFriendDependencies).map { Path(it) }
                if (dependencies.isEmpty()) return@l emptyList()
                val scope = projectEnvironment.getSearchScopeByClassPath(dependencies)
                val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(scope)
                val moduleData = moduleDataProvider.allModuleData.first { it.session == session }
                val provider = JvmClassFileBasedSymbolProvider(
                    session,
                    moduleDataProvider,
                    scopeProvider,
                    context.packagePartProviderForLibraries,
                    kotlinClassFinder,
                    projectEnvironment.getFirJavaFacade(session, moduleData, context.librariesScope)
                )
                val builtinsProvider = FirJvmSessionFactory.initializeBuiltinsProvider(
                    session,
                    moduleData,
                    scopeProvider,
                    kotlinClassFinder,
                )

                listOf(provider, builtinsProvider)
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
                    createJavaFacade = javaDirectFacade,
                )
            },
            createSourceSession = { moduleData, kmpModuleKind, sessionConfigurator ->
                FirJvmSessionFactory.createSourceSession(
                    moduleData,
                    javaSourcesScope,
                    createIncrementalCompilationSymbolProviders = ic@{ session ->
                        // TODO(OSIP-75): should be removed, see the comment above
                        if (isKmpCompilationWithLegacyIC) {
                            return@ic if (firJvmIncrementalCompilationSymbolProvidersIsInitialized) firJvmIncrementalCompilationSymbolProviders
                            else {
                                firJvmIncrementalCompilationSymbolProvidersIsInitialized = true
                                incrementalCompilationContext?.createSymbolProviders(session, moduleData, projectEnvironment)?.also {
                                    firJvmIncrementalCompilationSymbolProviders = it
                                }
                            }
                        }

                        when (kmpModuleKind) {
                            KmpModuleKind.SingleModule,
                            KmpModuleKind.LeafRegularModule,
                            KmpModuleKind.LeafHmppModule -> incrementalCompilationContext?.createSymbolProviders(
                                session,
                                moduleData,
                                projectEnvironment
                            )

                            KmpModuleKind.NonLeafRegularModule,
                            KmpModuleKind.NonLeafHmppModule -> createIncrementalProvidersForNonLeafMppModules(
                                session,
                                moduleData,
                                configuration
                            )
                        }
                    },
                    extensionRegistrars,
                    configuration,
                    context,
                    needRegisterJavaElementFinder = true,
                    kmpModuleKind = kmpModuleKind,
                    createJavaFacade = javaDirectFacade,
                    init = sessionConfigurator,
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
                    for (classpath in configuration.contentRoots) {
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
     * @return `null` if no applicable extensions were found, `true` if all applicable extensions returned `true` from [FirAnalysisHandlerExtension.doAnalysis],
     * `false` if any applicable extension returned `false`
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

    // ----------------------- Project environment utils -----------------------

    fun createProjectEnvironment(
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
        configFiles: EnvironmentConfigFiles,
    ): VfsBasedProjectEnvironment {
        setupIdeaStandaloneExecution()

        @OptIn(CoreEnvironmentDeprecation::class)
        val appEnv = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(parentDisposable, configuration)
        // TODO: get rid of projEnv too - seems that all needed components could be easily extracted
        val projectEnvironment = KotlinCoreEnvironment.ProjectEnvironment(parentDisposable, appEnv, configuration)

        @OptIn(CoreEnvironmentDeprecation::class)
        projectEnvironment.configureProjectEnvironment(configuration, configFiles)

        val project = projectEnvironment.project
        val localFileSystem = projectEnvironment.environment.localFileSystem

        val javaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        val jdkRelease = configuration.jdkRelease

        val javaModuleFinder = CliJavaModuleFinder(
            configuration.jdkHome,
            configuration,
            javaFileManager,
            project,
            jdkRelease
        )

        val outputDirectory = configuration.modules.singleOrNull()?.getOutputDirectory()
            ?: configuration.outputDirectory?.absolutePath

        val contentRoots = configuration.contentRoots

        val classpathRootsResolver = ClasspathRootsResolver(
            PsiManager.getInstance(project),
            configuration,
            additionalModules = configuration.additionalJavaModules,
            contentRootToVirtualFile = { contentRootToVirtualFile(it, localFileSystem, projectEnvironment.jarFileSystem, configuration) },
            javaModuleFinder,
            requireStdlibModule = !configuration.allowKotlinPackage,
            outputDirectory?.let { localFileSystem.findFileByPath(it) },
            javaFileManager,
            jdkRelease,
            hasKotlinSources = contentRoots.any { it is KotlinSourceRoot },
        )

        (val initialRoots = roots, val javaModules = modules) = classpathRootsResolver.convertClasspathRoots(contentRoots)

        val [roots, singleJavaFileRoots] = initialRoots.partition { (file) ->
            file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
        }

        // REPL updates classpath dynamically
        val rootsIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true).apply {
            addIndex(JvmDependenciesIndexImpl(roots))
            indexedRoots.forEach {
                projectEnvironment.addSourcesToClasspath(it.file)
            }
        }

        val perfManager = configuration.perfManager

        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(), project)
        )

        val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, enableSearchInCtSym = jdkRelease != null, perfManager)
        project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
        project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))

        project.setupHighestLanguageLevel()

        return ProjectEnvironmentWithCoreEnvironmentEmulation(
            project,
            listOfNotNull(projectEnvironment.jarFileSystem, projectEnvironment.environment.jrtFileSystem, localFileSystem),
            { JvmPackagePartProvider(configuration.languageVersionSettings, it) },
            initialRoots, configuration,
            projectEnvironment,
            classpathRootsResolver,
            rootsIndex
        ).also {
            javaFileManager.initialize(
                rootsIndex,
                it.packagePartProviders,
                SingleJavaFileRootsIndex(singleJavaFileRoots),
                configuration.usePsiClassFilesReading,
                perfManager,
            )
        }
    }

    private class ProjectEnvironmentWithCoreEnvironmentEmulation(
        project: Project,
        knownFileSystems: List<VirtualFileSystem>,
        getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
        initialRoots: List<JavaRoot>,
        val configuration: CompilerConfiguration,
        val projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
        val classpathRootsResolver: ClasspathRootsResolver,
        val rootsIndex: JvmDependenciesDynamicCompoundIndex
    ) : VfsBasedProjectEnvironment(project, knownFileSystems, getPackagePartProviderFn) {

        private val currentRoots = initialRoots.toMutableList()

        val packagePartProviders = mutableListOf<JvmPackagePartProvider>()

        override fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider {
            return super.getPackagePartProvider(fileSearchScope).also {
                (it as? JvmPackagePartProvider)?.run {
                    addRoots(currentRoots, configuration)
                    packagePartProviders += this
                }
            }
        }

        override fun updateClasspath(classpath: List<File>) {
            val contentRoots = classpath.map { JvmClasspathRoot(it) }
            val newRoots = classpathRootsResolver.convertClasspathRoots(contentRoots).roots - currentRoots
            val unindexedRoots = rootsIndex.getUnindexedRoots(newRoots)
            if (unindexedRoots.isEmpty()) return

            val newIndex = JvmDependenciesIndexImpl(unindexedRoots)
            rootsIndex.addIndex(newIndex)
            newIndex.indexedRoots.forEach {
                projectEnvironment.addSourcesToClasspath(it.file)
            }

            currentRoots.addAll(newRoots)
            for (packagePartProvider in packagePartProviders) {
                packagePartProvider.addRoots(newRoots, configuration)
            }

            configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, contentRoots - configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS))
        }
    }

    private fun contentRootToVirtualFile(
        root: JvmContentRootBase,
        localFileSystem: VirtualFileSystem,
        jarFileSystem: VirtualFileSystem,
        configuration: CompilerConfiguration,
    ): VirtualFile? {
        return when (root) {
            // TODO: find out why non-existent location is not reported for JARs, add comment or fix
            is JvmClasspathRoot ->
                if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
                else localFileSystem.findExistingRoot(root, "Classpath entry", configuration)
            is JvmModulePathRoot ->
                if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
                else localFileSystem.findExistingRoot(root, "Java module root", configuration)
            is JavaSourceRoot ->
                localFileSystem.findExistingRoot(root, "Java source root", configuration)
            is VirtualJvmClasspathRoot ->
                root.file
            else ->
                throw IllegalStateException("Unexpected root: $root")
        }
    }

    private fun VirtualFileSystem.findJarRoot(file: File): VirtualFile? {
        return findFileByPath("$file${URLUtil.JAR_SEPARATOR}")
    }

    private fun VirtualFileSystem.findExistingRoot(
        root: JvmContentRoot,
        rootDescription: String,
        configuration: CompilerConfiguration,
    ): VirtualFile? {
        return findFileByPath(root.file.absolutePath).also {
            if (it == null) {
                configuration.report(
                    ROOTS_RESOLUTION_WARNING,
                    "$rootDescription points to a non-existent location: ${root.file}"
                )
            }
        }
    }

    private fun createCoreEnvironment(
        rootDisposable: Disposable,
        configuration: CompilerConfiguration,
        targetDescription: String,
    ): KotlinCoreEnvironment? {
        val perfManager = configuration.perfManager
        perfManager?.targetDescription = targetDescription

        if (CheckDiagnosticCollector.checkHasErrors(configuration)) return null

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        val sourceFiles = environment.getSourceFiles()
        perfManager?.addSourcesStats(sourceFiles.size, environment.countLinesOfCode(sourceFiles))

        return if (CheckDiagnosticCollector.checkHasErrors(configuration)) null else environment
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

private fun ModuleChunk.targetDescription(): String {
    return modules
        .map { input -> input.getModuleName() + "-" + input.getModuleType() }
        .let { names -> names.singleOrNull() ?: names.joinToString() }
}
