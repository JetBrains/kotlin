/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline

import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
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
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File

@LegacyK2CliPipeline
fun convertAnalyzedFirToIr(
    configuration: CompilerConfiguration,
    targetId: TargetId,
    frontendOutput: AllModulesFrontendOutput,
    environment: ModuleCompilerEnvironment
): ModuleCompilerIrBackendInput {
    val extensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())

    val (moduleFragment, components, pluginContext, irActualizedResult, _, symbolTable) =
        frontendOutput.convertToIrAndActualizeForJvm(
            extensions, configuration, environment.diagnosticsReporter,
            configuration.getCompilerExtensions(IrGenerationExtension),
        )

    return ModuleCompilerIrBackendInput(
        targetId,
        configuration,
        extensions,
        moduleFragment,
        components,
        pluginContext,
        irActualizedResult,
        symbolTable
    )
}

fun AllModulesFrontendOutput.convertToIrAndActualizeForJvm(
    fir2IrExtensions: Fir2IrExtensions,
    configuration: CompilerConfiguration,
    diagnosticsReporter: BaseDiagnosticsCollector,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
): Fir2IrActualizedResult {
    val fir2IrConfiguration = Fir2IrConfiguration.forJvmCompilation(configuration, diagnosticsReporter)

    return convertToIrAndActualize(
        fir2IrExtensions,
        fir2IrConfiguration,
        irGeneratorExtensions,
        JvmIrMangler,
        FirJvmVisibilityConverter,
        DefaultBuiltIns.Instance,
        ::JvmIrTypeSystemContext,
        JvmIrSpecialAnnotationSymbolProvider,
        if (configuration.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            { emptyList() }
        } else {
            { listOfNotNull(FirDirectJavaActualDeclarationExtractor.initializeIfNeeded(it)) }
        },
    )
}

@LegacyK2CliPipeline
fun generateCodeFromIr(
    input: ModuleCompilerIrBackendInput,
    environment: ModuleCompilerEnvironment
): GenerationState {
    val generationState = GenerationState(
        environment.projectEnvironment.project,
        input.irModuleFragment.descriptor,
        input.configuration,
        ClassBuilderFactories.BINARIES,
        targetId = input.targetId,
        moduleName = input.targetId.name,
        jvmBackendClassResolver = FirJvmBackendClassResolver(input.components),
        diagnosticReporter = environment.diagnosticsReporter,
    )

    val performanceManager = input.configuration.perfManager
    @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
    performanceManager?.notifyCurrentPhaseFinishedIfNeeded() // It should be `notifyIRGenerationFinished`, but this phase not always started or already finished
    lateinit var codegenFactory: JvmIrCodegenFactory
    val codegenInput = performanceManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
        val backendInput = JvmIrCodegenFactory.BackendInput(
            input.irModuleFragment,
            input.pluginContext.irBuiltIns,
            input.symbolTable,
            input.components.irProviders,
            input.extensions,
            FirJvmBackendExtension(
                input.components,
                input.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            ),
            input.pluginContext,
        )

        codegenFactory = JvmIrCodegenFactory(input.configuration)
        codegenFactory.invokeLowerings(generationState, backendInput)
    }

    codegenFactory.invokeCodegen(codegenInput)

    // It's allowed to call `tryMeasurePhaseTime` multiple times on the same phase (`Backend`)
    performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {
        if (input.configuration.outputDirectory != null) {
            writeOutputsIfNeeded(
                environment.projectEnvironment.project,
                input.configuration,
                input.configuration.messageCollector,
                environment.diagnosticsReporter.hasErrors,
                listOf(generationState),
                mainClassFqName = null,
            )
        }
    }

    return generationState
}

private class ProjectEnvironmentWithCoreEnvironmentEmulation(
    project: Project,
    knownFileSystems: List<VirtualFileSystem>,
    getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
    val initialRoots: List<JavaRoot>,
    val configuration: CompilerConfiguration
) : VfsBasedProjectEnvironment(project, knownFileSystems, getPackagePartProviderFn) {

    val packagePartProviders = mutableListOf<JvmPackagePartProvider>()

    override fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider {
        return super.getPackagePartProvider(fileSearchScope).also {
            (it as? JvmPackagePartProvider)?.run {
                addRoots(initialRoots, configuration)
                packagePartProviders += this
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
    val appEnv = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(parentDisposable, configuration)
    // TODO: get rid of projEnv too - seems that all needed components could be easily extracted
    val projectEnvironment = KotlinCoreEnvironment.ProjectEnvironment(parentDisposable, appEnv, configuration)

    projectEnvironment.configureProjectEnvironment(configuration, configFiles)

    val project = projectEnvironment.project
    val localFileSystem = projectEnvironment.environment.localFileSystem

    val javaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

    val releaseTarget = configuration.get(JVMConfigurationKeys.JDK_RELEASE)

    val javaModuleFinder = CliJavaModuleFinder(
        configuration.get(JVMConfigurationKeys.JDK_HOME),
        configuration,
        javaFileManager,
        project,
        releaseTarget
    )

    val outputDirectory =
        configuration.get(JVMConfigurationKeys.MODULES)?.singleOrNull()?.getOutputDirectory()
            ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.absolutePath

    val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)

    val classpathRootsResolver = ClasspathRootsResolver(
        PsiManager.getInstance(project),
        configuration,
        configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES),
        { contentRootToVirtualFile(it, localFileSystem, projectEnvironment.jarFileSystem, messageCollector) },
        javaModuleFinder,
        !configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE),
        outputDirectory?.let { localFileSystem.findFileByPath(it) },
        javaFileManager,
        releaseTarget,
        hasKotlinSources = contentRoots.any { it is KotlinSourceRoot },
    )

    val (initialRoots, javaModules) =
        classpathRootsResolver.convertClasspathRoots(contentRoots)

    val (roots, singleJavaFileRoots) =
        initialRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

    // REPL and kapt2 update classpath dynamically
    val rootsIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true).apply {
        addIndex(JvmDependenciesIndexImpl(roots, shouldOnlyFindFirstClass = true))
        indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
//            javaFileManager.addToClasspath(it.file)
        }
    }

    val perfManager = configuration.perfManager

    project.registerService(
        JavaModuleResolver::class.java,
        CliJavaModuleResolver(classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(), project)
    )

    val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, releaseTarget != null, perfManager)
    project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
    project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))

    project.setupHighestLanguageLevel()

    return ProjectEnvironmentWithCoreEnvironmentEmulation(
        project,
        listOfNotNull(projectEnvironment.jarFileSystem, projectEnvironment.environment.jrtFileSystem, localFileSystem),
        { JvmPackagePartProvider(configuration.languageVersionSettings, it) },
        initialRoots, configuration
    ).also {
        javaFileManager.initialize(
            rootsIndex,
            it.packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING),
            perfManager,
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
        is VirtualJvmClasspathRoot ->
            root.file
        else ->
            throw IllegalStateException("Unexpected root: $root")
    }

private fun VirtualFileSystem.findJarRoot(file: File): VirtualFile? =
    findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

private fun VirtualFileSystem.findExistingRoot(
    root: JvmContentRoot, rootDescription: String, messageCollector: MessageCollector,
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
