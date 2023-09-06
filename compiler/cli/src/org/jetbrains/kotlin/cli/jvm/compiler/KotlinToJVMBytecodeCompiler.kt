/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.runFrontend
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.LOOKUP_TRACKER
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

object KotlinToJVMBytecodeCompiler {
    internal fun compileModules(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
        repeat: Boolean = false
    ): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val compilerConfiguration = environment.configuration
        val repeats = compilerConfiguration[CLIConfigurationKeys.REPEAT_COMPILE_MODULES]
        if (repeats != null && !repeat) {
            val performanceManager = compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]
            return (0 until repeats).map {
                val result = compileModules(environment, buildFile, chunk, repeat = true)
                performanceManager?.notifyRepeat(repeats, it)
                result
            }.last()
        }

        val project = environment.project
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = compilerConfiguration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        val useFrontendIR = compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR)
        val messageCollector = environment.messageCollector
        val (codegenFactory, wholeBackendInput, moduleDescriptor, bindingContext, firJvmBackendResolver, firJvmBackendExtension, mainClassFqName) = if (useFrontendIR) {
            // K2/PSI: base checks
            val projectEnvironment =
                VfsBasedProjectEnvironment(
                    project,
                    VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
                ) { environment.createPackagePartProvider(it) }

            if (!FirKotlinToJvmBytecodeCompiler.checkNotSupportedPlugins(compilerConfiguration, messageCollector)) {
                return false
            }

            // K2/PSI: single module chunk mode fallback (KT-61745)
            if (chunk.size == 1) {
                return FirKotlinToJvmBytecodeCompiler.compileModulesUsingFrontendIRAndPsi(
                    projectEnvironment,
                    compilerConfiguration,
                    messageCollector,
                    environment.getSourceFiles(),
                    buildFile,
                    chunk.single()
                )
            }

            runFrontendAndGenerateIrForMultiModuleChunkUsingFrontendIR(environment, projectEnvironment, compilerConfiguration, chunk)
        } else {
            runFrontendAndGenerateIrUsingClassicFrontend(environment, compilerConfiguration, chunk)
        } ?: return false
        // K1/K2 common multi-chunk part
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()

        val codegenInputs = ArrayList<CodegenFactory.CodegenInput>(chunk.size)

        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = module.getSourceFiles(environment.getSourceFiles(), localFileSystem, chunk.size > 1, buildFile)
            if (!checkKotlinPackageUsageForPsi(compilerConfiguration, ktFiles)) return false
            val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)

            val backendInput = codegenFactory.getModuleChunkBackendInput(wholeBackendInput, ktFiles).let {
                if (it is JvmIrCodegenFactory.JvmIrBackendInput && firJvmBackendExtension != null) {
                    it.copy(backendExtension = firJvmBackendExtension)
                } else it
            }
            // Lowerings (per module)
            codegenInputs += runLowerings(
                environment, moduleConfiguration, moduleDescriptor, bindingContext,
                ktFiles, module, codegenFactory, backendInput, diagnosticsReporter, firJvmBackendResolver
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            // Codegen (per module)
            outputs += runCodegen(input, input.state, codegenFactory, bindingContext, diagnosticsReporter, compilerConfiguration)
        }

        return writeOutputsIfNeeded(project, compilerConfiguration, messageCollector, outputs, mainClassFqName)
    }

    private fun runFrontendAndGenerateIrForMultiModuleChunkUsingFrontendIR(
        environment: KotlinCoreEnvironment,
        projectEnvironment: VfsBasedProjectEnvironment,
        compilerConfiguration: CompilerConfiguration,
        chunk: List<Module>,
    ): BackendInputForMultiModuleChunk? {
        val sourceFiles = environment.getSourceFiles()
        val project = projectEnvironment.project
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
        val frontendContext = FirKotlinToJvmBytecodeCompiler.FrontendContextForMultiChunkMode(
            projectEnvironment, environment, compilerConfiguration, project
        )

        with(frontendContext) {
            // K2/PSI: frontend
            val firResult = runFrontend(
                sourceFiles, diagnosticsReporter, chunk.joinToString(separator = "+") { it.getModuleName() },
                chunk.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() }
            ) ?: run {
                FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                    diagnosticsReporter, messageCollector,
                    compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
                )
                return null
            }
            // K2/PSI: FIR2IR
            val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)
            val irGenerationExtensions = IrGenerationExtension.getInstances(project)
            val fir2IrAndIrActualizerResult = convertToIrAndActualizeForJvm(
                firResult, diagnosticsReporter, fir2IrExtensions, irGenerationExtensions
            )
            val (factory, input) = fir2IrAndIrActualizerResult.codegenFactoryWithJvmIrBackendInput(configuration)
            return BackendInputForMultiModuleChunk(
                factory,
                input,
                fir2IrAndIrActualizerResult.irModuleFragment.descriptor,
                NoScopeRecordCliBindingTrace().bindingContext,
                FirJvmBackendClassResolver(fir2IrAndIrActualizerResult.components),
                FirJvmBackendExtension(
                    fir2IrAndIrActualizerResult.components, fir2IrAndIrActualizerResult.irActualizedResult
                )
            )
        }
    }

    private fun runFrontendAndGenerateIrUsingClassicFrontend(
        environment: KotlinCoreEnvironment,
        compilerConfiguration: CompilerConfiguration,
        chunk: List<Module>
    ): BackendInputForMultiModuleChunk? {
        // K1: Frontend
        val result = repeatAnalysisIfNeeded(analyze(environment), environment)
        if (result == null || !result.shouldGenerateCode) return null

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val mainClassFqName = runIf(chunk.size == 1 && compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(
                result.bindingContext, compilerConfiguration.languageVersionSettings, environment.getSourceFiles()
            )
        }

        // K1: PSI2IR
        val (factory, input) = convertToIr(environment, result)
        return BackendInputForMultiModuleChunk(
            factory,
            input,
            result.moduleDescriptor,
            result.bindingContext,
            mainClassFqName = mainClassFqName
        )
    }

    private data class BackendInputForMultiModuleChunk(
        val codegenFactory: CodegenFactory,
        val backendInput: CodegenFactory.BackendInput,
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext,
        val firJvmBackendClassResolver: FirJvmBackendClassResolver? = null,
        val firJvmBackendExtension: FirJvmBackendExtension? = null,
        val mainClassFqName: FqName? = null,
    )

    fun compileBunchOfSources(environment: KotlinCoreEnvironment): Boolean {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        if (!checkKotlinPackageUsageForPsi(environment.configuration, environment.getSourceFiles())) return false

        val generationState = analyzeAndGenerate(environment) ?: return false

        try {
            writeOutput(environment.configuration, generationState.factory, null)
            return true
        } finally {
            generationState.destroy()
        }
    }

    private fun repeatAnalysisIfNeeded(result: AnalysisResult?, environment: KotlinCoreEnvironment): AnalysisResult? {
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            val configuration = environment.configuration

            val oldReadOnlyValue = configuration.isReadOnly
            configuration.isReadOnly = false
            configuration.addJavaSourceRoots(result.additionalJavaRoots)
            configuration.isReadOnly = oldReadOnlyValue

            if (result.addToEnvironment) {
                environment.updateClasspath(result.additionalJavaRoots.map { JavaSourceRoot(it, null) })
            }

            if (result.additionalClassPathRoots.isNotEmpty()) {
                environment.updateClasspath(result.additionalClassPathRoots.map { JvmClasspathRoot(it, false) })
            }

            if (result.additionalKotlinRoots.isNotEmpty()) {
                environment.addKotlinSourceRoots(result.additionalKotlinRoots)
            }

            KotlinJavaPsiFacade.getInstance(environment.project).clearPackageCaches()

            val javaClassesTracker = configuration[JAVA_CLASSES_TRACKER]
            javaClassesTracker?.clear()

            val lookupTracker = configuration[LOOKUP_TRACKER]
            lookupTracker?.clear()

            // Clear all diagnostic messages
            configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]?.clear()

            // Repeat analysis with additional source roots generated by compiler plugins.
            return repeatAnalysisIfNeeded(analyze(environment), environment)
        }

        return result
    }

    @Suppress("MemberVisibilityCanBePrivate") // Used in ExecuteKotlinScriptMojo
    fun analyzeAndGenerate(environment: KotlinCoreEnvironment): GenerationState? {
        val result = repeatAnalysisIfNeeded(analyze(environment), environment) ?: return null

        if (!result.shouldGenerateCode) return null

        result.throwIfError()

        val (codegenFactory, backendInput) = convertToIr(environment, result)
        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()
        val input = runLowerings(
            environment, environment.configuration, result.moduleDescriptor, result.bindingContext,
            environment.getSourceFiles(), null, codegenFactory, backendInput, diagnosticsReporter
        )
        return runCodegen(input, input.state, codegenFactory, result.bindingContext, diagnosticsReporter, environment.configuration)
    }

    private fun convertToIr(environment: KotlinCoreEnvironment, result: AnalysisResult): Pair<CodegenFactory, CodegenFactory.BackendInput> {
        val configuration = environment.configuration
        val codegenFactory =
            if (configuration.getBoolean(JVMConfigurationKeys.IR)) JvmIrCodegenFactory(
                configuration, configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
            ) else DefaultCodegenFactory

        val input = CodegenFactory.IrConversionInput(
            environment.project,
            environment.getSourceFiles(),
            configuration,
            result.moduleDescriptor,
            result.bindingContext,
            configuration.languageVersionSettings,
            ignoreErrors = false,
            skipBodies = false,
        )

        val performanceManager = environment.configuration[CLIConfigurationKeys.PERF_MANAGER]
        performanceManager?.notifyIRTranslationStarted()
        val backendInput = codegenFactory.convertToIr(input)
        performanceManager?.notifyIRTranslationFinished()

        return Pair(codegenFactory, backendInput)
    }

    private fun Fir2IrActualizedResult.codegenFactoryWithJvmIrBackendInput(
        configuration: CompilerConfiguration
    ): Pair<CodegenFactory, CodegenFactory.BackendInput> {
        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        return codegenFactory to JvmIrCodegenFactory.JvmIrBackendInput(
            irModuleFragment,
            components.symbolTable,
            phaseConfig,
            components.irProviders,
            JvmGeneratorExtensionsImpl(configuration),
            JvmBackendExtension.Default,
            pluginContext,
        ) {}
    }

    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val collector = environment.messageCollector
        val sourceFiles = environment.getSourceFiles()

        // Can be null for Scripts/REPL
        val performanceManager = environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager?.notifyAnalysisStarted()

        val resolvedKlibs = environment.configuration.get(JVMConfigurationKeys.KLIB_PATHS)?.let { klibPaths ->
            jvmResolveLibraries(klibPaths, collector.toLogger())
        }?.getFullList() ?: emptyList()

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
            collector,
            environment.configuration.languageVersionSettings,
            environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
        analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
            val project = environment.project
            val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)?.mapNotNullTo(hashSetOf()) { module ->
                environment.findLocalFile(module.getOutputDirectory())
            }.orEmpty()
            val sourcesOnly = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            // To support partial and incremental compilation, we add the scope which contains binaries from output directories
            // of the compiled modules (.class) to the list of scopes of the source module
            val scope = if (moduleOutputs.isEmpty()) sourcesOnly else sourcesOnly.uniteWith(DirectoriesScope(project, moduleOutputs))
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                sourceFiles,
                NoScopeRecordCliBindingTrace(),
                environment.configuration,
                environment::createPackagePartProvider,
                sourceModuleSearchScope = scope,
                klibList = resolvedKlibs
            )
        }

        performanceManager?.notifyAnalysisFinished()

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalRoots)
            analysisResult
        else
            null
    }

    class DirectoriesScope(
        project: Project,
        private val directories: Set<VirtualFile>
    ) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        override fun contains(file: VirtualFile): Boolean {
            if (file.fileSystem !in fileSystems) return false

            var parent: VirtualFile = file
            while (true) {
                if (parent in directories) return true
                parent = parent.parent ?: return false
            }
        }

        override fun toString() = "All files under: $directories"
    }

    private fun runLowerings(
        environment: KotlinCoreEnvironment,
        configuration: CompilerConfiguration,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        sourceFiles: List<KtFile>,
        module: Module?,
        codegenFactory: CodegenFactory,
        backendInput: CodegenFactory.BackendInput,
        diagnosticsReporter: BaseDiagnosticsCollector,
        firJvmBackendClassResolver: FirJvmBackendClassResolver? = null,
    ): CodegenFactory.CodegenInput {
        val performanceManager = environment.configuration[CLIConfigurationKeys.PERF_MANAGER]

        val state = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.BINARIES,
            moduleDescriptor,
            bindingContext,
            configuration
        )
            .withModule(module)
            .onIndependentPartCompilationEnd(createOutputFilesFlushingCallbackIfPossible(configuration))
            .diagnosticReporter(diagnosticsReporter)
            .apply {
                if (firJvmBackendClassResolver != null) {
                    jvmBackendClassResolver(firJvmBackendClassResolver)
                }
            }.build()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        performanceManager?.notifyGenerationStarted()

        state.beforeCompile()
        state.oldBEInitTrace(sourceFiles)

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        performanceManager?.notifyIRLoweringStarted()
        return codegenFactory.invokeLowerings(state, backendInput)
            .also { performanceManager?.notifyIRLoweringFinished() }
    }

    private fun runCodegen(
        codegenInput: CodegenFactory.CodegenInput,
        state: GenerationState,
        codegenFactory: CodegenFactory,
        bindingContext: BindingContext,
        diagnosticsReporter: BaseDiagnosticsCollector,
        configuration: CompilerConfiguration,
    ): GenerationState {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]

        performanceManager?.notifyIRGenerationStarted()
        codegenFactory.invokeCodegen(codegenInput)

        CodegenFactory.doCheckCancelled(state)
        state.factory.done()
        performanceManager?.notifyIRGenerationFinished()

        performanceManager?.notifyGenerationFinished()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        AnalyzerWithCompilerReport.reportDiagnostics(
            FilteredJvmDiagnostics(
                state.collectedExtraJvmDiagnostics,
                bindingContext.diagnostics
            ),
            messageCollector,
            configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
            diagnosticsReporter,
            messageCollector,
            configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return state
    }
}

fun CompilerConfiguration.configureSourceRoots(chunk: List<Module>, buildFile: File? = null) {
    val hmppCliModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    for (module in chunk) {
        val commonSources = getBuildFilePaths(buildFile, module.getCommonSourceFiles()).toSet()

        for (path in getBuildFilePaths(buildFile, module.getSourceFiles())) {
            addKotlinSourceRoot(path, isCommon = path in commonSources, hmppCliModuleStructure?.getModuleNameForSource(path))
        }
    }

    for (module in chunk) {
        for ((path, packagePrefix) in module.getJavaSourceRoots()) {
            addJavaSourceRoot(File(path), packagePrefix)
        }
    }

    val isJava9Module = chunk.any { module ->
        module.getJavaSourceRoots().any { (path, packagePrefix) ->
            val file = File(path)
            packagePrefix == null &&
                    (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                            (file.isDirectory && file.listFiles()!!.any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
        }
    }

    for (module in chunk) {
        for (classpathRoot in module.getClasspathRoots()) {
            if (isJava9Module) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(File(classpathRoot)))
            }
            add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(classpathRoot)))
        }
    }

    for (module in chunk) {
        val modularJdkRoot = module.modularJdkRoot
        if (modularJdkRoot != null) {
            // We use the SDK of the first module in the chunk, which is not always correct because some other module in the chunk
            // might depend on a different SDK
            put(JVMConfigurationKeys.JDK_HOME, File(modularJdkRoot))
            break
        }
    }

    addAll(JVMConfigurationKeys.MODULES, chunk)
}
