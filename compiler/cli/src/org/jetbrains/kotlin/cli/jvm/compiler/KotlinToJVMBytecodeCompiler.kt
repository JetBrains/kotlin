/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import java.io.File

object KotlinToJVMBytecodeCompiler {
    internal fun compileModules(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
        repeat: Boolean = false
    ): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val repeats = environment.configuration[CLIConfigurationKeys.REPEAT_COMPILE_MODULES]
        if (repeats != null && !repeat) {
            val performanceManager = environment.configuration[CLIConfigurationKeys.PERF_MANAGER]
            return (0 until repeats).map {
                val result = compileModules(environment, buildFile, chunk, repeat = true)
                performanceManager?.notifyRepeat(repeats, it)
                result
            }.last()
        }

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        val projectConfiguration = environment.configuration
        if (projectConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            val extendedAnalysisMode = projectConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS)
            val projectEnvironment =
                PsiBasedProjectEnvironment(
                    environment.project,
                    VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                    { environment.createPackagePartProvider(it) }
                )
            return FirKotlinToJvmBytecodeCompiler.compileModulesUsingFrontendIR(
                projectEnvironment,
                environment.configuration,
                environment.messageCollector,
                environment.getSourceFiles(),
                buildFile, chunk, extendedAnalysisMode
            )
        }

        val result = repeatAnalysisIfNeeded(analyze(environment), environment)
        if (result == null || !result.shouldGenerateCode) return false

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val mainClassFqName =
            if (chunk.size == 1 && projectConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null)
                findMainClass(result.bindingContext, projectConfiguration.languageVersionSettings, environment.getSourceFiles())
            else null

        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val (codegenFactory, wholeBackendInput) = convertToIr(environment, result)
        val diagnosticsReporter = DiagnosticReporterFactory.createReporter()

        val codegenInputs = ArrayList<CodegenFactory.CodegenInput>(chunk.size)

        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = module.getSourceFiles(environment.getSourceFiles(), localFileSystem, chunk.size > 1, buildFile)
            if (!checkKotlinPackageUsage(environment.configuration, ktFiles)) return false
            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)

            val backendInput = codegenFactory.getModuleChunkBackendInput(wholeBackendInput, ktFiles)
            codegenInputs += runLowerings(
                environment, moduleConfiguration, result, ktFiles, module, codegenFactory, backendInput, diagnosticsReporter
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            outputs += runCodegen(input, input.state, result.bindingContext, diagnosticsReporter, environment.configuration)
        }

        return writeOutputs(environment.project, projectConfiguration, chunk, outputs, mainClassFqName)
    }

    internal fun configureSourceRoots(configuration: CompilerConfiguration, chunk: List<Module>, buildFile: File? = null) {
        for (module in chunk) {
            val commonSources = getBuildFilePaths(buildFile, module.getCommonSourceFiles()).toSet()

            for (path in getBuildFilePaths(buildFile, module.getSourceFiles())) {
                configuration.addKotlinSourceRoot(path, isCommon = path in commonSources)
            }
        }

        for (module in chunk) {
            for ((path, packagePrefix) in module.getJavaSourceRoots()) {
                configuration.addJavaSourceRoot(File(path), packagePrefix)
            }
        }

        val isJava9Module = chunk.any { module ->
            module.getJavaSourceRoots().any { (path, packagePrefix) ->
                val file = File(path)
                packagePrefix == null &&
                        (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                                (file.isDirectory && file.listFiles().any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
            }
        }

        for (module in chunk) {
            for (classpathRoot in module.getClasspathRoots()) {
                configuration.add(
                    CLIConfigurationKeys.CONTENT_ROOTS,
                    if (isJava9Module) JvmModulePathRoot(File(classpathRoot)) else JvmClasspathRoot(File(classpathRoot))
                )
            }
        }

        for (module in chunk) {
            val modularJdkRoot = module.modularJdkRoot
            if (modularJdkRoot != null) {
                // We use the SDK of the first module in the chunk, which is not always correct because some other module in the chunk
                // might depend on a different SDK
                configuration.put(JVMConfigurationKeys.JDK_HOME, File(modularJdkRoot))
                break
            }
        }

        configuration.addAll(JVMConfigurationKeys.MODULES, chunk)
    }

    fun compileBunchOfSources(environment: KotlinCoreEnvironment): Boolean {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        if (!checkKotlinPackageUsage(environment.configuration, environment.getSourceFiles())) return false

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
            environment, environment.configuration, result, environment.getSourceFiles(), null, codegenFactory, backendInput,
            diagnosticsReporter
        )
        return runCodegen(input, input.state, result.bindingContext, diagnosticsReporter, environment.configuration)
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
            false,
        )
        val backendInput = codegenFactory.convertToIr(input)
        return Pair(codegenFactory, backendInput)
    }

    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val sourceFiles = environment.getSourceFiles()
        val collector = environment.messageCollector

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
        result: AnalysisResult,
        sourceFiles: List<KtFile>,
        module: Module?,
        codegenFactory: CodegenFactory,
        backendInput: CodegenFactory.BackendInput,
        diagnosticsReporter: BaseDiagnosticsCollector,
    ): CodegenFactory.CodegenInput {
        val state = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.BINARIES,
            result.moduleDescriptor,
            result.bindingContext,
            sourceFiles,
            configuration
        )
            .codegenFactory(codegenFactory)
            .withModule(module)
            .onIndependentPartCompilationEnd(createOutputFilesFlushingCallbackIfPossible(configuration))
            .diagnosticReporter(diagnosticsReporter)
            .build()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)?.notifyGenerationStarted()

        state.beforeCompile()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return codegenFactory.invokeLowerings(state, backendInput)
    }

    private fun runCodegen(
        codegenInput: CodegenFactory.CodegenInput,
        state: GenerationState,
        bindingContext: BindingContext,
        diagnosticsReporter: BaseDiagnosticsCollector,
        configuration: CompilerConfiguration,
    ): GenerationState {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        state.codegenFactory.invokeCodegen(codegenInput)

        CodegenFactory.doCheckCancelled(state)
        state.factory.done()

        configuration.get(CLIConfigurationKeys.PERF_MANAGER)?.notifyGenerationFinished()

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
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector)

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return state
    }
}
