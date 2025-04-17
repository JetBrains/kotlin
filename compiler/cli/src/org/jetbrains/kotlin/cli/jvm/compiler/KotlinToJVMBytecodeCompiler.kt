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
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.toLogger
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.JvmBackendClassResolverForModuleWithDependencies
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.LOOKUP_TRACKER
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

object KotlinToJVMBytecodeCompiler {
    var customClassBuilderFactory = CompilerConfigurationKey.create<ClassBuilderFactory>("Custom ClassBuilderFactory")

    internal fun compileModules(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>
    ): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val compilerConfiguration = environment.configuration

        val project = environment.project
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = compilerConfiguration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        check(compilerConfiguration.useFir == false)
        val messageCollector = environment.messageCollector
        val diagnosticsReporter = DiagnosticReporterFactory.createReporter(messageCollector)
        val backendInputForMultiModuleChunk =
            runFrontendAndGenerateIrUsingClassicFrontend(environment, compilerConfiguration, chunk, diagnosticsReporter) ?: return true

        return backendInputForMultiModuleChunk.runBackend(
            project,
            chunk,
            compilerConfiguration,
            messageCollector,
            diagnosticsReporter,
            buildFile,
            allSourceFiles = environment.getSourceFiles(),
        )
    }

    internal fun BackendInputForMultiModuleChunk.runBackend(
        project: Project,
        chunk: List<Module>,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        diagnosticsReporter: BaseDiagnosticsCollector,
        buildFile: File?,
        allSourceFiles: List<KtFile>?,
    ): Boolean {
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val codegenInputs = ArrayList<JvmIrCodegenFactory.CodegenInput>(chunk.size)
        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = if (allSourceFiles != null) {
                val sourceFiles = module.getSourceFiles(allSourceFiles, localFileSystem, chunk.size > 1, buildFile)
                if (!checkKotlinPackageUsageForPsi(compilerConfiguration, sourceFiles)) return false
                sourceFiles
            } else null

            val moduleConfiguration = compilerConfiguration.createConfigurationForModule(module, buildFile)
            val backendInput = (if (ktFiles != null) {
                codegenFactory.getModuleChunkBackendInput(backendInput, ktFiles)
            } else {
                val wholeModule = backendInput.irModuleFragment
                val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor)
                wholeModule.files.filterTo(moduleCopy.files) { file ->
                    file.fileEntry.name in module.getSourceFiles()
                }
                backendInput.copy(moduleCopy)
            }).let {
                if (firJvmBackendExtension != null) {
                    it.copy(backendExtension = firJvmBackendExtension)
                } else it
            }
            // Lowerings (per module)
            codegenInputs += runLowerings(
                project, moduleConfiguration, moduleDescriptor, module, codegenFactory, backendInput, diagnosticsReporter,
                firJvmBackendClassResolver,
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            // Codegen (per module)
            outputs += runCodegen(
                input,
                input.state,
                codegenFactory,
                diagnosticsReporter,
                compilerConfiguration,
                reportDiagnosticsToMessageCollector = true,
            )
        }

        return writeOutputsIfNeeded(
            project,
            compilerConfiguration,
            messageCollector,
            hasPendingErrors = false,
            outputs,
            mainClassFqName
        )
    }

    private fun runFrontendAndGenerateIrUsingClassicFrontend(
        environment: KotlinCoreEnvironment,
        compilerConfiguration: CompilerConfiguration,
        chunk: List<Module>,
        diagnosticsReporter: DiagnosticReporter
    ): BackendInputForMultiModuleChunk? {
        // K1: Frontend
        val result = environment.configuration.perfManager.let {
            it?.notifyPhaseFinished(PhaseType.Initialization)
            it.tryMeasurePhaseTime(PhaseType.Analysis) {
                repeatAnalysisIfNeeded(analyze(environment), environment)
            }
        }
        if (result == null || !result.shouldGenerateCode) return null

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val mainClassFqName = runIf(chunk.size == 1 && compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(
                result.bindingContext, compilerConfiguration.languageVersionSettings, environment.getSourceFiles()
            )
        }

        // K1: PSI2IR
        val (factory, input) = convertToIr(environment, result, diagnosticsReporter)
        return BackendInputForMultiModuleChunk(factory, input, result.moduleDescriptor, mainClassFqName = mainClassFqName)
    }

    internal data class BackendInputForMultiModuleChunk(
        val codegenFactory: JvmIrCodegenFactory,
        val backendInput: JvmIrCodegenFactory.BackendInput,
        val moduleDescriptor: ModuleDescriptor,
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

        writeOutput(environment.configuration, generationState.factory, null)
        return true
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
            configuration.messageCollector.clear()

            // Repeat analysis with additional source roots generated by compiler plugins.
            return repeatAnalysisIfNeeded(analyze(environment), environment)
        }

        return result
    }

    @Suppress("MemberVisibilityCanBePrivate") // Used in ExecuteKotlinScriptMojo
    fun analyzeAndGenerate(environment: KotlinCoreEnvironment): GenerationState? {
        val result = environment.configuration.perfManager.let {
            it?.notifyPhaseFinished(PhaseType.Initialization)
            it.tryMeasurePhaseTime(PhaseType.Analysis) {
                repeatAnalysisIfNeeded(analyze(environment), environment) ?: return null
            }
        }

        if (!result.shouldGenerateCode) return null

        result.throwIfError()

        val messageCollector = environment.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val diagnosticsReporter = DiagnosticReporterFactory.createReporter(messageCollector)
        val (codegenFactory, backendInput) = convertToIr(environment, result, diagnosticsReporter)
        val input = runLowerings(
            environment.project, environment.configuration, result.moduleDescriptor, module = null, codegenFactory,
            backendInput, diagnosticsReporter,
        )
        return runCodegen(
            input,
            input.state,
            codegenFactory,
            diagnosticsReporter,
            environment.configuration,
            reportDiagnosticsToMessageCollector = true,
        )
    }

    private fun convertToIr(
        environment: KotlinCoreEnvironment,
        result: AnalysisResult,
        diagnosticsReporter: DiagnosticReporter
    ): Pair<JvmIrCodegenFactory, JvmIrCodegenFactory.BackendInput> {
        val configuration = environment.configuration
        val codegenFactory = JvmIrCodegenFactory(configuration)
        val performanceManager = environment.configuration[CLIConfigurationKeys.PERF_MANAGER]
        val backendInput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            codegenFactory.convertToIr(
                environment.project,
                environment.getSourceFiles(),
                configuration,
                result.moduleDescriptor,
                diagnosticsReporter,
                result.bindingContext,
                configuration.languageVersionSettings,
                ignoreErrors = false,
                skipBodies = false,
            )
        }
        return Pair(codegenFactory, backendInput)
    }

    internal fun Fir2IrActualizedResult.toBackendInput(
        configuration: CompilerConfiguration,
        jvmBackendExtension: JvmBackendExtension?
    ): JvmIrCodegenFactory.BackendInput {
        return JvmIrCodegenFactory.BackendInput(
            irModuleFragment,
            irBuiltIns,
            symbolTable,
            components.irProviders,
            JvmGeneratorExtensionsImpl(configuration),
            jvmBackendExtension ?: JvmBackendExtension.Default,
            pluginContext,
        )
    }

    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val collector = environment.messageCollector
        val sourceFiles = environment.getSourceFiles()

        // TODO: use KLIB loader instead of KLIB resolver
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
                NoScopeRecordCliBindingTrace(project),
                environment.configuration,
                environment::createPackagePartProvider,
                sourceModuleSearchScope = scope,
                klibList = resolvedKlibs
            )
        }

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalRoots)
            analysisResult
        else
            null
    }

    class DirectoriesScope(
        project: Project,
        private val directories: Set<VirtualFile>
    ) : DelegatingGlobalSearchScope(allScope(project)) {
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

    internal fun runLowerings(
        project: Project,
        configuration: CompilerConfiguration,
        moduleDescriptor: ModuleDescriptor,
        module: Module?,
        codegenFactory: JvmIrCodegenFactory,
        backendInput: JvmIrCodegenFactory.BackendInput,
        diagnosticsReporter: BaseDiagnosticsCollector,
        firJvmBackendClassResolver: FirJvmBackendClassResolver? = null,
    ): JvmIrCodegenFactory.CodegenInput {
        val state = GenerationState(
            project,
            moduleDescriptor,
            configuration,
            builderFactory = configuration.get(customClassBuilderFactory, ClassBuilderFactories.BINARIES),
            targetId = module?.let(::TargetId),
            moduleName = module?.getModuleName() ?: configuration.moduleName,
            diagnosticReporter = diagnosticsReporter,
            jvmBackendClassResolver = firJvmBackendClassResolver ?: JvmBackendClassResolverForModuleWithDependencies(moduleDescriptor),
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            codegenFactory.invokeLowerings(state, backendInput)
        }
    }

    internal fun runCodegen(
        codegenInput: JvmIrCodegenFactory.CodegenInput,
        state: GenerationState,
        codegenFactory: JvmIrCodegenFactory,
        diagnosticsReporter: BaseDiagnosticsCollector,
        configuration: CompilerConfiguration,
        reportDiagnosticsToMessageCollector: Boolean,
    ): GenerationState {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]

        performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {
            codegenFactory.invokeCodegen(codegenInput)
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (reportDiagnosticsToMessageCollector) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                diagnosticsReporter,
                configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY),
                configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return state
    }
}

fun CompilerConfiguration.configureSourceRoots(chunk: List<Module>, buildFile: File? = null) {
    val hmppCliModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    for (module in chunk) {
        val commonSources = getBuildFilePaths(buildFile, module.getCommonSourceFiles()).toSet()

        for (path in getBuildFilePaths(buildFile, module.getSourceFiles())) {
            addKotlinSourceRoot(
                path,
                isCommon = hmppCliModuleStructure?.isFromCommonModule(path) ?: (path in commonSources),
                hmppCliModuleStructure?.getModuleNameForSource(path)
            )
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
