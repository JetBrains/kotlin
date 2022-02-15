/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.configureProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.convertToIr
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

fun compileModulesUsingFrontendIrAndLightTree(
    projectEnvironment: AbstractProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    buildFile: File?,
    chunk: List<Module>,
    targetDescription: String
): Boolean {
    require(projectEnvironment is VfsBasedProjectEnvironment) // TODO: abstract away this requirement
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    val performanceManager = compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]

    performanceManager?.notifyCompilerInitialized(0, 0, targetDescription)

    messageCollector.report(
        CompilerMessageSeverity.STRONG_WARNING,
        "ATTENTION!\n This build uses in-dev FIR: \n  -Xuse-fir"
    )

    val outputs = mutableListOf<GenerationState>()
    var mainClassFqName: FqName? = null

    for (module in chunk) {
        val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)
        val platformSources = linkedSetOf<File>()
        val commonSources = linkedSetOf<File>()

        // !!
        compilerConfiguration.kotlinSourceRoots.forAllFiles(compilerConfiguration, projectEnvironment.project) { virtualFile, isCommon ->
            val file = File(virtualFile.canonicalPath ?: virtualFile.path)
            if (!file.isFile) error("TODO: better error: file not found $virtualFile")
            if (isCommon) commonSources.add(file)
            else platformSources.add(file)
        }

        val renderDiagnosticName = moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()

        val compilerInput = ModuleCompilerInput(
            TargetId(module),
            CommonPlatforms.defaultCommonPlatform, commonSources,
            JvmPlatforms.unspecifiedJvmPlatform, platformSources,
            moduleConfiguration
        )
        val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)

        performanceManager?.notifyAnalysisStarted()

        val analysisResults = compileModuleToAnalyzedFir(
            compilerInput,
            compilerEnvironment,
            emptyList(),
            null,
            diagnosticsReporter,
            performanceManager
        )

        performanceManager?.notifyAnalysisFinished()

        // TODO: consider what to do if many modules has main classes
        if (mainClassFqName == null && moduleConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            mainClassFqName = findMainClass(analysisResults.fir)
        }

        if (diagnosticsReporter.hasErrors) {
            diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
            continue
        }

        performanceManager?.notifyGenerationStarted()
        performanceManager?.notifyIRTranslationStarted()

        val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment)

        performanceManager?.notifyIRTranslationFinished()

        val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment, performanceManager)

        diagnosticsReporter.reportToMessageCollector(
            messageCollector, moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )

        performanceManager?.notifyIRGenerationFinished()
        performanceManager?.notifyGenerationFinished()

        if (!diagnosticsReporter.hasErrors) {
            outputs.add(codegenOutput.generationState)
        }
    }

    return writeOutputs(
        projectEnvironment,
        compilerConfiguration,
        outputs,
        mainClassFqName
    )
}

fun convertAnalyzedFirToIr(
    input: ModuleCompilerInput,
    analysisResults: ModuleCompilerAnalyzedOutput,
    environment: ModuleCompilerEnvironment
): ModuleCompilerIrBackendInput {
    val extensions = JvmGeneratorExtensionsImpl(input.configuration)

    // fir2ir
    val irGenerationExtensions =
        (environment.projectEnvironment as? VfsBasedProjectEnvironment)?.project?.let { IrGenerationExtension.getInstances(it) }
    val (irModuleFragment, symbolTable, components) =
        analysisResults.session.convertToIr(
            analysisResults.scopeSession, analysisResults.fir, extensions, irGenerationExtensions ?: emptyList()
        )

    return ModuleCompilerIrBackendInput(
        input.targetId,
        input.configuration,
        extensions,
        irModuleFragment,
        symbolTable,
        components,
        analysisResults.session
    )
}

fun generateCodeFromIr(
    input: ModuleCompilerIrBackendInput,
    environment: ModuleCompilerEnvironment,
    performanceManager: CommonCompilerPerformanceManager?
): ModuleCompilerOutput {
    // IR
    val codegenFactory = JvmIrCodegenFactory(
        input.configuration,
        input.configuration.get(CLIConfigurationKeys.PHASE_CONFIG),
        jvmGeneratorExtensions = input.extensions
    )
    val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

    val generationState = GenerationState.Builder(
        (environment.projectEnvironment as VfsBasedProjectEnvironment).project, ClassBuilderFactories.BINARIES,
        input.irModuleFragment.descriptor, dummyBindingContext, emptyList()/* !! */,
        input.configuration
    ).targetId(
        input.targetId
    ).moduleName(
        input.targetId.name
    ).outDirectory(
        input.configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
    ).onIndependentPartCompilationEnd(
        createOutputFilesFlushingCallbackIfPossible(input.configuration)
    ).isIrBackend(
        true
    ).jvmBackendClassResolver(
        FirJvmBackendClassResolver(input.components)
    ).diagnosticReporter(
        environment.diagnosticsReporter
    ).build()

    performanceManager?.notifyIRLoweringStarted()

    generationState.beforeCompile()
    codegenFactory.generateModuleInFrontendIRMode(
        generationState, input.irModuleFragment, input.symbolTable, input.extensions,
        FirJvmBackendExtension(input.firSession, input.components)
    ) {
        performanceManager?.notifyIRLoweringFinished()
        performanceManager?.notifyIRGenerationStarted()
    }
    CodegenFactory.doCheckCancelled(generationState)
    generationState.factory.done()

    return ModuleCompilerOutput(generationState)
}

fun compileModuleToAnalyzedFir(
    input: ModuleCompilerInput,
    environment: ModuleCompilerEnvironment,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
    diagnosticsReporter: DiagnosticReporter,
    performanceManager: CommonCompilerPerformanceManager?
): ModuleCompilerAnalyzedOutput {
    var sourcesScope = environment.projectEnvironment.getSearchScopeByIoFiles(input.platformSources) //!!
    val sessionProvider = FirProjectSessionProvider()
    val extendedAnalysisMode = input.configuration.getBoolean(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS)

    val commonSession = runIf(
        input.commonSources.isNotEmpty() && input.configuration.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
    ) {
        val commonSourcesScope = environment.projectEnvironment.getSearchScopeByIoFiles(input.commonSources) //!!
        sourcesScope -= commonSourcesScope
        createSession(
            "${input.targetId.name}-common",
            input.commonPlatform,
            input.configuration,
            environment.projectEnvironment,
            commonSourcesScope,
            CommonPlatformAnalyzerServices,
            sessionProvider,
            previousStepsSymbolProviders,
            incrementalExcludesScope,
            extendedAnalysisMode
        )
    }

    val session = createSession(
        input.targetId.name,
        input.platform,
        input.configuration,
        environment.projectEnvironment,
        sourcesScope,
        JvmPlatformAnalyzerServices,
        sessionProvider,
        previousStepsSymbolProviders,
        incrementalExcludesScope,
        extendedAnalysisMode
    ) {
        if (commonSession != null) {
            sourceDependsOnDependencies(listOf(commonSession.moduleData))
        }
        friendDependencies(input.configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
        sourceFriendsDependencies(input.friendFirModules)
    }

    val countFilesAndLines = if (performanceManager == null) null else performanceManager::addSourcesStats

    // raw fir
    val commonRawFir = commonSession?.buildFirViaLightTree(
        input.commonSources,
        environment.projectEnvironment,
        diagnosticsReporter,
        countFilesAndLines
    )
    val rawFir =
        session.buildFirViaLightTree(input.platformSources, environment.projectEnvironment, diagnosticsReporter, countFilesAndLines)

    // resolution
    commonSession?.apply {
        val (commonScopeSession, commonFir) = runResolution(commonRawFir!!)
        // TODO: find out what to do with commonFir
        runCheckers(commonScopeSession, commonFir, environment.diagnosticsReporter)
    }

    val (scopeSession, fir) = session.runResolution(rawFir)
    // checkers
    session.runCheckers(scopeSession, fir, environment.diagnosticsReporter)

    return ModuleCompilerAnalyzedOutput(session, scopeSession, fir)
}

fun writeOutputs(
    projectEnvironment: AbstractProjectEnvironment,
    configuration: CompilerConfiguration,
    outputs: Collection<GenerationState>,
    mainClassFqName: FqName?
): Boolean {
    try {
        for (state in outputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            writeOutput(state.configuration, state.factory, mainClassFqName)
        }
    } finally {
        outputs.forEach(GenerationState::destroy)
    }

    if (configuration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)) {
        val singleState = outputs.singleOrNull()
        if (singleState != null) {
            return JavacWrapper.getInstance((projectEnvironment as VfsBasedProjectEnvironment).project).use {
                it.compile(singleState.outDirectory)
            }
        } else {
            configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                CompilerMessageSeverity.WARNING,
                "A chunk contains multiple modules (${outputs.joinToString { it.moduleName }}). " +
                        "-Xuse-javac option couldn't be used to compile java files"
            )
        }
    }

    return true
}


fun createSession(
    name: String,
    platform: TargetPlatform,
    moduleConfiguration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    analyzerServices: PlatformDependentAnalyzerServices,
    sessionProvider: FirProjectSessionProvider?,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
    extendedAnalysisMode: Boolean,
    dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {},
): FirSession {
    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val providerAndScopeForIncrementalCompilation =
        createContextForIncrementalCompilation(
            moduleConfiguration,
            projectEnvironment,
            sourceScope,
            previousStepsSymbolProviders,
            incrementalExcludesScope
        )
            ?.also { (_, _, precompiledBinariesFileScope) ->
                precompiledBinariesFileScope?.let { librariesScope -= it }
            }

    return FirSessionFactory.createSessionWithDependencies(
        Name.identifier(name),
        platform,
        analyzerServices,
        externalSessionProvider = sessionProvider,
        projectEnvironment,
        moduleConfiguration.languageVersionSettings,
        projectEnvironment.getSearchScopeForProjectJavaSources(),
        librariesScope,
        lookupTracker = moduleConfiguration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
        providerAndScopeForIncrementalCompilation,
        extensionRegistrars = (projectEnvironment as? VfsBasedProjectEnvironment)?.let { FirExtensionRegistrar.getInstances(it.project) }
            ?: emptyList(),
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

private fun createContextForIncrementalCompilation(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
): FirSessionFactory.IncrementalCompilationContext? {
    val targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
    val incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
    if (targetIds == null || incrementalComponents == null) return null
    val incrementalCompilationScope =
        compilerConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]?.let { dir ->
            projectEnvironment.getSearchScopeByDirectories(setOf(dir)).let {
                if (incrementalExcludesScope?.isEmpty != false) it
                else it - incrementalExcludesScope
            }
        }

    return if (incrementalCompilationScope == null && previousStepsSymbolProviders.isEmpty()) null
    else FirSessionFactory.IncrementalCompilationContext(
        previousStepsSymbolProviders, IncrementalPackagePartProvider(
            projectEnvironment.getPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        ), incrementalCompilationScope
    )
}

private class ProjectEnvironmentWithCoreEnvironmentEmulation(
    project: Project,
    localFileSystem: VirtualFileSystem,
    getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
    val initialRoots: List<JavaRoot>,
    val configuration: CompilerConfiguration
) : VfsBasedProjectEnvironment(project, localFileSystem, getPackagePartProviderFn) {

    val packagePartProviders = mutableListOf<JvmPackagePartProvider>()

    override fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider {
        return super.getPackagePartProvider(fileSearchScope).also {
            (it as? JvmPackagePartProvider)?.run {
                (it as? JvmPackagePartProvider)?.run {
                    addRoots(initialRoots, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
                    packagePartProviders += this
                    (ModuleAnnotationsResolver.getInstance(project) as CliModuleAnnotationsResolver).addPackagePartProvider(this)
                }
            }
        }
    }
}

fun createProjectEnvironment(
    configuration: CompilerConfiguration,
    parentDisposable: Disposable,
    configFiles: EnvironmentConfigFiles,
    messageCollector: MessageCollector
): VfsBasedProjectEnvironment {
    setupIdeaStandaloneExecution()
    val appEnv = KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(parentDisposable, configuration)
    // TODO: get rid of projEnv too - seems that all needed components could be easily extracted
    val projectEnvironment = KotlinCoreEnvironment.ProjectEnvironment(parentDisposable, appEnv, configuration)

    projectEnvironment.configureProjectEnvironment(configuration, configFiles)

    val project = projectEnvironment.project
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val javaFileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

    val releaseTarget = configuration.get(JVMConfigurationKeys.JDK_RELEASE)

    val javaModuleFinder =
        CliJavaModuleFinder(configuration.get(JVMConfigurationKeys.JDK_HOME), messageCollector, javaFileManager, project, releaseTarget)

    val outputDirectory =
        configuration.get(JVMConfigurationKeys.MODULES)?.singleOrNull()?.getOutputDirectory()
            ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.absolutePath

    val classpathRootsResolver = ClasspathRootsResolver(
        PsiManager.getInstance(project),
        messageCollector,
        configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES),
        { contentRootToVirtualFile(it, localFileSystem, projectEnvironment.jarFileSystem, messageCollector) },
        javaModuleFinder,
        !configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE),
        outputDirectory?.let { localFileSystem.findFileByPath(it) },
        javaFileManager,
        releaseTarget
    )

    val (initialRoots, javaModules) =
        classpathRootsResolver.convertClasspathRoots(configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS))

    val (roots, singleJavaFileRoots) =
        initialRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

    // REPL and kapt2 update classpath dynamically
    val rootsIndex = JvmDependenciesDynamicCompoundIndex().apply {
        addIndex(JvmDependenciesIndexImpl(roots))
        indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
//            javaFileManager.addToClasspath(it.file)
        }
    }

    project.registerService(
        JavaModuleResolver::class.java,
        CliJavaModuleResolver(classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(), project)
    )

    val finderFactory = CliVirtualFileFinderFactory(rootsIndex, releaseTarget != null)
    project.registerService(MetadataFinderFactory::class.java, finderFactory)
    project.registerService(VirtualFileFinderFactory::class.java, finderFactory)

    return ProjectEnvironmentWithCoreEnvironmentEmulation(
        project,
        localFileSystem,
        { JvmPackagePartProvider(configuration.languageVersionSettings, it) },
        initialRoots, configuration
    ).also {
        javaFileManager.initialize(
            rootsIndex,
            it.packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING)
        )
    }
}

private fun contentRootToVirtualFile(
    root: JvmContentRootBase, locaFileSystem: VirtualFileSystem, jarFileSystem: VirtualFileSystem, messageCollector: MessageCollector
): VirtualFile? =
    when (root) {
        // TODO: find out why non-existent location is not reported for JARs, add comment or fix
        is JvmClasspathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
            else locaFileSystem.findExistingRoot(root, "Classpath entry", messageCollector)
        is JvmModulePathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
            else locaFileSystem.findExistingRoot(root, "Java module root", messageCollector)
        is JavaSourceRoot ->
            locaFileSystem.findExistingRoot(root, "Java source root", messageCollector)
        else ->
            throw IllegalStateException("Unexpected root: $root")
    }

private fun VirtualFileSystem.findJarRoot(file: File): VirtualFile? =
    findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

private fun VirtualFileSystem.findExistingRoot(
    root: JvmContentRoot, rootDescription: String, messageCollector: MessageCollector
): VirtualFile? {
    return findFileByPath(root.file.absolutePath).also {
        if (it == null) {
            messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "$rootDescription points to a non-existent location: ${root.file}"
            )
        }
    }
}
