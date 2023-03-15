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
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.*
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
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
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
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File
import kotlin.reflect.KFunction2

private const val kotlinFileExtensionWithDot = ".${KotlinFileType.EXTENSION}"
private const val javaFileExtensionWithDot = ".${JavaFileType.DEFAULT_EXTENSION}"

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

    val outputs = mutableListOf<GenerationState>()
    var mainClassFqName: FqName? = null

    for (module in chunk) {
        val moduleConfiguration = compilerConfiguration.copy().applyModuleProperties(module, buildFile).apply {
            put(JVMConfigurationKeys.FRIEND_PATHS, module.getFriendPaths())
        }
        val groupedSources = collectSources(compilerConfiguration, projectEnvironment, messageCollector)

        val compilerInput = ModuleCompilerInput(
            TargetId(module),
            groupedSources,
            CommonPlatforms.defaultCommonPlatform,
            JvmPlatforms.unspecifiedJvmPlatform,
            moduleConfiguration
        )

        val renderDiagnosticName = moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
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
            mainClassFqName = findMainClass(analysisResults.outputs.last().fir)
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

data class GroupedKtSources(
    val platformSources: Collection<KtSourceFile>,
    val commonSources: Collection<KtSourceFile>,
    val sourcesByModuleName: Map<String, Set<KtSourceFile>>,
)

fun collectSources(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector
): GroupedKtSources {
    val platformSources = linkedSetOf<KtSourceFile>()
    val commonSources = linkedSetOf<KtSourceFile>()
    val sourcesByModuleName = mutableMapOf<String, MutableSet<KtSourceFile>>()

    // TODO: the scripts checking should be part of the scripting plugin functionality, as it is implemented now in ScriptingProcessSourcesBeforeCompilingExtension
    // TODO: implement in the next round of K2 scripting support (https://youtrack.jetbrains.com/issue/KT-55728)
    val skipScriptsInLtMode = compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR) &&
            compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
    var skipScriptsInLtModeWarning = false

    compilerConfiguration.kotlinSourceRoots.forAllFiles(
        compilerConfiguration,
        projectEnvironment.project
    ) { virtualFile, isCommon, moduleName ->
        val file = KtVirtualFileSourceFile(virtualFile)
        when {
            file.path.endsWith(javaFileExtensionWithDot) -> {}
            file.path.endsWith(kotlinFileExtensionWithDot) || !skipScriptsInLtMode -> {
                if (isCommon) commonSources.add(file)
                else platformSources.add(file)

                if (moduleName != null) {
                    sourcesByModuleName.getOrPut(moduleName) { mutableSetOf() }.add(file)
                }
            }
            else -> {
                // temporarily assume it is a script, see the TODO above
                skipScriptsInLtModeWarning = true
            }
        }
    }

    if (skipScriptsInLtModeWarning) {
        // TODO: remove then Scripts are supported in LT (probably different K2 extension should be written for handling the case properly)
        messageCollector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Scripts are not yet supported with K2 in LightTree mode, consider using K1 or disable LightTree mode with -Xuse-fir-lt=false"
        )
    }
    return GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
}

fun convertAnalyzedFirToIr(
    input: ModuleCompilerInput,
    analysisResults: FirResult,
    environment: ModuleCompilerEnvironment
): ModuleCompilerIrBackendInput {
    val extensions = JvmFir2IrExtensions(input.configuration, JvmIrDeserializerImpl(), JvmIrMangler)

    // fir2ir
    val irGenerationExtensions =
        (environment.projectEnvironment as? VfsBasedProjectEnvironment)?.project?.let {
            IrGenerationExtension.getInstances(it)
        } ?: emptyList()
    val linkViaSignatures = input.configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
    val (irModuleFragment, components, pluginContext, irActualizationResult) =
        analysisResults.convertToIrAndActualizeForJvm(
            extensions, irGenerationExtensions, linkViaSignatures,
            environment.diagnosticsReporter, input.configuration.languageVersionSettings
        )

    return ModuleCompilerIrBackendInput(
        input.targetId,
        input.configuration,
        extensions,
        irModuleFragment,
        components,
        pluginContext,
        irActualizationResult
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
    )
    val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

    val generationState = GenerationState.Builder(
        (environment.projectEnvironment as VfsBasedProjectEnvironment).project, ClassBuilderFactories.BINARIES,
        input.irModuleFragment.descriptor, dummyBindingContext, input.configuration
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
        generationState,
        input.irModuleFragment,
        input.components.symbolTable,
        input.components.irProviders,
        input.extensions,
        FirJvmBackendExtension(input.components, input.irActualizationResult),
        input.pluginContext
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
): FirResult {
    val projectEnvironment = environment.projectEnvironment
    val moduleConfiguration = input.configuration

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
    val rootModuleName = input.targetId.name

    val incrementalCompilationScope = createIncrementalCompilationScope(
        moduleConfiguration,
        projectEnvironment,
        incrementalExcludesScope
    )?.also { librariesScope -= it }

    val extensionRegistrars = (projectEnvironment as? VfsBasedProjectEnvironment)
        ?.let { FirExtensionRegistrar.getInstances(it.project) }
        ?: emptyList()

    val allSources = mutableListOf<KtSourceFile>().apply {
        addAll(input.groupedSources.commonSources)
        addAll(input.groupedSources.platformSources)
    }
    // TODO: handle friends paths
    val libraryList = createLibraryListForJvm(rootModuleName, moduleConfiguration, friendPaths = emptyList())
    val sessionWithSources = prepareJvmSessions(
        allSources, moduleConfiguration, projectEnvironment, Name.identifier(rootModuleName),
        extensionRegistrars, librariesScope, libraryList,
        isCommonSource = input.groupedSources.isCommonSourceForLt,
        fileBelongsToModule = input.groupedSources.fileBelongsToModuleForLt,
        createProviderAndScopeForIncrementalCompilation = { files ->
            val scope = projectEnvironment.getSearchScopeBySourceFiles(files)
            createContextForIncrementalCompilation(
                moduleConfiguration,
                projectEnvironment,
                scope,
                previousStepsSymbolProviders,
                incrementalCompilationScope
            )
        }
    )

    val countFilesAndLines = if (performanceManager == null) null else performanceManager::addSourcesStats

    val outputs = sessionWithSources.map { (session, sources) ->
        buildResolveAndCheckFir(session, sources, diagnosticsReporter, countFilesAndLines)
    }

    return FirResult(outputs)
}

private fun buildResolveAndCheckFir(
    session: FirSession,
    ktFiles: Collection<KtSourceFile>,
    diagnosticsReporter: DiagnosticReporter,
    countFilesAndLines: KFunction2<Int, Int, Unit>?
): ModuleCompilerAnalyzedOutput {
    val firFiles = session.buildFirViaLightTree(ktFiles, diagnosticsReporter, countFilesAndLines)
    return resolveAndCheckFir(session, firFiles, diagnosticsReporter)
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

fun createIncrementalCompilationScope(
    configuration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    incrementalExcludesScope: AbstractProjectFileSearchScope?
): AbstractProjectFileSearchScope? {
    if (!needCreateIncrementalCompilationScope(configuration)) return null
    val dir = configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY] ?: return null
    return projectEnvironment.getSearchScopeByDirectories(setOf(dir)).let {
        if (incrementalExcludesScope?.isEmpty != false) it
        else it - incrementalExcludesScope
    }
}

private fun needCreateIncrementalCompilationScope(configuration: CompilerConfiguration, ): Boolean {
    if (configuration.get(JVMConfigurationKeys.MODULES) == null) return false
    if (configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS) == null) return false
    return true
}

fun createContextForIncrementalCompilation(
    configuration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalCompilationScope: AbstractProjectFileSearchScope?
): IncrementalCompilationContext? {
    if (incrementalCompilationScope == null && previousStepsSymbolProviders.isEmpty()) return null
    val targetIds = configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId) ?: return null
    val incrementalComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS) ?: return null

    return IncrementalCompilationContext(
        previousStepsSymbolProviders,
        IncrementalPackagePartProvider(
            projectEnvironment.getPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        ),
        incrementalCompilationScope
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
                addRoots(initialRoots, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
                packagePartProviders += this
                (ModuleAnnotationsResolver.getInstance(project) as CliModuleAnnotationsResolver).addPackagePartProvider(this)
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

    project.setupHighestLanguageLevel()

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
    root: JvmContentRootBase,
    localFileSystem: VirtualFileSystem,
    jarFileSystem: VirtualFileSystem,
    messageCollector: MessageCollector,
): VirtualFile? =
    when (root) {
        // TODO: find out why non-existent location is not reported for JARs, add comment or fix
        is JvmClasspathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
            else localFileSystem.findExistingRoot(root, "Classpath entry", messageCollector)
        is JvmModulePathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file)
            else localFileSystem.findExistingRoot(root, "Java module root", messageCollector)
        is JavaSourceRoot ->
            localFileSystem.findExistingRoot(root, "Java source root", messageCollector)
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
