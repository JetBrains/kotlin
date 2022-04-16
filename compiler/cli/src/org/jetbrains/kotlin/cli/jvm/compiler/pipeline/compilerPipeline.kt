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
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
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
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.convertToIr
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
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
    chunk: List<Module>
): Boolean {
    require(projectEnvironment is VfsBasedProjectEnvironment) // TODO: abstract away this requirement
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    messageCollector.report(
        CompilerMessageSeverity.STRONG_WARNING,
        "ATTENTION!\n This build uses in-dev FIR: \n  -Xuse-fir"
    )

    val outputs = mutableMapOf<Module, GenerationState>()
    var mainClassFqName: FqName? = null

    for (module in chunk) {
        val moduleConfiguration = compilerConfiguration.copy().applyModuleProperties(module, buildFile).apply {
            addAll(JVMConfigurationKeys.FRIEND_PATHS, module.getFriendPaths())
        }
        val platformSources = linkedSetOf<File>()
        val commonSources = linkedSetOf<File>()

        // !!
        compilerConfiguration.kotlinSourceRoots.forAllFiles(compilerConfiguration, projectEnvironment.project) { virtualFile, isCommon ->
            val file = File(virtualFile.canonicalPath ?: virtualFile.path)
            if (!file.isFile) error("TODO: better error: file not found $virtualFile")
            if (isCommon) commonSources.add(file)
            else platformSources.add(file)
        }

        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()

        val (moduleMainClassName, generationState) = compileModule(
            ModuleCompilerInput(
                TargetId(module),
                CommonPlatforms.defaultCommonPlatform, commonSources,
                JvmPlatforms.unspecifiedJvmPlatform, platformSources,
                moduleConfiguration
            ),
            ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)
        )
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
            diagnosticsReporter,
            messageCollector,
            moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
        outputs[module] = generationState

        // TODO: consider what to do if many modules contain main class
        if (mainClassFqName == null) {
            mainClassFqName = moduleMainClassName
        }
    }

    return writeOutputs(
        (projectEnvironment as? VfsBasedProjectEnvironment)?.project,
        compilerConfiguration,
        chunk,
        outputs,
        mainClassFqName
    )
}

fun compileModule(
    input: ModuleCompilerInput,
    environment: ModuleCompilerEnvironment
): ModuleCompilerOutput {
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
            extendedAnalysisMode,
            needRegisterJavaElementFinder = false
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
        extendedAnalysisMode,
        needRegisterJavaElementFinder = true
    ) {
        if (commonSession != null) {
            sourceDependsOnDependencies(listOf(commonSession.moduleData))
        }
        friendDependencies(input.configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
    }

    // raw fir
    val commonRawFir = commonSession?.buildFirViaLightTree(input.commonSources)
    val rawFir = session.buildFirViaLightTree(input.platformSources)

    // resolution
    commonSession?.apply {
        val (commonScopeSession, commonFir) = runResolution(commonRawFir!!)
        // TODO: find out what to do with commonFir
        runCheckers(commonScopeSession, commonFir, environment.diagnosticsReporter)
    }

    val (scopeSession, fir) = session.runResolution(rawFir)
    // checkers
    session.runCheckers(scopeSession, fir, environment.diagnosticsReporter)

    val mainClassFqName: FqName? = runIf(input.configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
        findMainClass(fir)
    }

    val extensions = JvmGeneratorExtensionsImpl(input.configuration)

    // fir2ir
    val irGenerationExtensions = (environment.projectEnvironment as? VfsBasedProjectEnvironment)?.project?.let { IrGenerationExtension.getInstances(it) }
    val (irModuleFragment, symbolTable, components) = session.convertToIr(scopeSession, fir, extensions, irGenerationExtensions ?: emptyList())

    // IR
    val codegenFactory = JvmIrCodegenFactory(
        input.configuration,
        input.configuration.get(CLIConfigurationKeys.PHASE_CONFIG),
        jvmGeneratorExtensions = extensions
    )
    val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

    val generationState = GenerationState.Builder(
        (environment.projectEnvironment as VfsBasedProjectEnvironment).project, ClassBuilderFactories.BINARIES,
        irModuleFragment.descriptor, dummyBindingContext, emptyList()/* !! */,
        input.configuration
    ).codegenFactory(
        codegenFactory
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
        FirJvmBackendClassResolver(components)
    ).diagnosticReporter(
        environment.diagnosticsReporter
    ).build()

    generationState.beforeCompile()
    codegenFactory.generateModuleInFrontendIRMode(
        generationState, irModuleFragment, symbolTable, extensions, FirJvmBackendExtension(session, components)
    )
    CodegenFactory.doCheckCancelled(generationState)
    generationState.factory.done()

    return ModuleCompilerOutput(mainClassFqName, generationState)
}

fun createSession(
    name: String,
    platform: TargetPlatform,
    moduleConfiguration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    analyzerServices: PlatformDependentAnalyzerServices,
    sessionProvider: FirProjectSessionProvider?,
    extendedAnalysisMode: Boolean,
    needRegisterJavaElementFinder: Boolean,
    dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {},
): FirSession {
    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val providerAndScopeForIncrementalCompilation =
        createComponentsForIncrementalCompilation(moduleConfiguration, projectEnvironment, sourceScope)?.also {
            librariesScope -= it.scope
        }

    return FirSessionFactory.createSessionWithDependencies(
        Name.identifier(name),
        platform,
        analyzerServices,
        externalSessionProvider = sessionProvider,
        projectEnvironment,
        moduleConfiguration.languageVersionSettings,
        sourceScope,
        librariesScope,
        lookupTracker = moduleConfiguration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
        providerAndScopeForIncrementalCompilation,
        extensionRegistrars = (projectEnvironment as? VfsBasedProjectEnvironment)?.let { FirExtensionRegistrar.getInstances(it.project) }
            ?: emptyList(),
        needRegisterJavaElementFinder = needRegisterJavaElementFinder,
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

private fun createComponentsForIncrementalCompilation(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope
): FirSessionFactory.ProviderAndScopeForIncrementalCompilation? {
    val targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
    val incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
    if (targetIds == null || incrementalComponents == null) return null
    val directoryWithIncrementalPartsFromPreviousCompilation =
        compilerConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
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

private fun contentRootToVirtualFile(root: JvmContentRoot, locaFileSystem: VirtualFileSystem, jarFileSystem: VirtualFileSystem, messageCollector: MessageCollector): VirtualFile? =
    when (root) {
        // TODO: find out why non-existent location is not reported for JARs, add comment or fix
        is JvmClasspathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file) else locaFileSystem.findExistingRoot(root, "Classpath entry", messageCollector)
        is JvmModulePathRoot ->
            if (root.file.isFile) jarFileSystem.findJarRoot(root.file) else locaFileSystem.findExistingRoot(root, "Java module root", messageCollector)
        is JavaSourceRoot ->
            locaFileSystem.findExistingRoot(root, "Java source root", messageCollector)
        else ->
            throw IllegalStateException("Unexpected root: $root")
    }

private fun VirtualFileSystem.findJarRoot(file: File): VirtualFile? =
    findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

private fun VirtualFileSystem.findExistingRoot(root: JvmContentRoot, rootDescription: String, messageCollector: MessageCollector): VirtualFile? {
    return findFileByPath(root.file.absolutePath).also {
        if (it == null) {
            messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "$rootDescription points to a non-existent location: ${root.file}"
            )
        }
    }
}
