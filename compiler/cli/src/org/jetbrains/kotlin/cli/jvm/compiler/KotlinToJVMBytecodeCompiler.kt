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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAll
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.tryConstructClassFromStringArgs
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

object KotlinToJVMBytecodeCompiler {

    private fun getAbsolutePaths(directory: File, module: Module): List<String> {
        return module.getSourceFiles().map { sourceFile ->
            var source = File(sourceFile)
            if (!source.isAbsolute) {
                source = File(directory, sourceFile)
            }
            source.absolutePath
        }
    }

    private fun writeOutput(
            configuration: CompilerConfiguration,
            outputFiles: OutputFileCollection,
            mainClass: FqName?
    ) {
        val reportOutputFiles = configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        val jarPath = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        if (jarPath != null) {
            val includeRuntime = configuration.get(JVMConfigurationKeys.INCLUDE_RUNTIME, false)
            CompileEnvironmentUtil.writeToJar(jarPath, includeRuntime, mainClass, outputFiles)
            if (reportOutputFiles) {
                val message = OutputMessageUtil.formatOutputMessage(outputFiles.asList().flatMap { it.sourceFiles }.distinct(), jarPath)
                messageCollector.report(OUTPUT, message)
            }
            return
        }

        val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) ?: File(".")
        outputFiles.writeAll(outputDir, messageCollector, reportOutputFiles)
    }

    private fun createOutputFilesFlushingCallbackIfPossible(configuration: CompilerConfiguration): GenerationStateEventCallback {
        if (configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) == null) {
            return GenerationStateEventCallback.DO_NOTHING
        }
        return GenerationStateEventCallback { state ->
            val currentOutput = SimpleOutputFileCollection(state.factory.currentOutput)
            writeOutput(configuration, currentOutput, mainClass = null)
            if (!configuration.get(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, false)) {
                state.factory.releaseGeneratedOutput()
            }
        }
    }

    fun compileModules(environment: KotlinCoreEnvironment, directory: File): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        val projectConfiguration = environment.configuration
        val chunk = projectConfiguration.getNotNull(JVMConfigurationKeys.MODULES)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        val targetDescription = "in targets [" + chunk.joinToString { input -> input.getModuleName() + "-" + input.getModuleType() } + "]"

        val result = repeatAnalysisIfNeeded(analyze(environment, targetDescription), environment, targetDescription)
        if (result == null || !result.shouldGenerateCode) return false

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val outputs = newLinkedHashMapWithExpectedSize<Module, GenerationState>(chunk.size)

        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            val ktFiles = CompileEnvironmentUtil.getKtFiles(
                    environment.project, getAbsolutePaths(directory, module), projectConfiguration
            ) { path -> throw IllegalStateException("Should have been checked before: $path") }
            if (!checkKotlinPackageUsage(environment, ktFiles)) return false

            val moduleConfiguration = projectConfiguration.copy().apply {
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(module.getOutputDirectory()))
            }

            outputs[module] = generate(environment, moduleConfiguration, result, ktFiles, module)
        }

        try {
            for ((_, state) in outputs) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                writeOutput(state.configuration, state.factory, null)
            }

            if (projectConfiguration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)) {
                val singleModule = chunk.singleOrNull()
                if (singleModule != null) {
                    return JavacWrapper.getInstance(environment.project).use {
                        it.compile(File(singleModule.getOutputDirectory()))
                    }
                }
                else {
                    projectConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).let {
                        it.report(WARNING, "A chunk contains multiple modules (${chunk.joinToString { it.getModuleName() }}). -Xuse-javac option couldn't be used to compile java files")
                    }
                    JavacWrapper.getInstance(environment.project).close()
                }
            }

            return true
        }
        finally {
            outputs.values.forEach(GenerationState::destroy)
        }
    }

    fun configureSourceRoots(configuration: CompilerConfiguration, chunk: List<Module>, directory: File) {
        for (module in chunk) {
            configuration.addKotlinSourceRoots(getAbsolutePaths(directory, module))
        }

        for (module in chunk) {
            for ((path, packagePrefix) in module.getJavaSourceRoots()) {
                configuration.addJavaSourceRoot(File(path), packagePrefix)
            }
        }

        val isJava9Module = false /*chunk.any { module ->
            module.getJavaSourceRoots().any { (path, packagePrefix) ->
                val file = File(path)
                packagePrefix == null &&
                (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                 (file.isDirectory && file.listFiles().any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
            }
        }*/

        for (module in chunk) {
            for (classpathRoot in module.getClasspathRoots()) {
                configuration.add(
                        JVMConfigurationKeys.CONTENT_ROOTS,
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

    private fun findMainClass(generationState: GenerationState, files: List<KtFile>): FqName? {
        val mainFunctionDetector = MainFunctionDetector(generationState.bindingContext)
        return files.asSequence()
                .map { file ->
                    if (mainFunctionDetector.hasMain(file.declarations))
                        JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
                    else
                        null
                }
                .singleOrNull { it != null }
    }

    fun compileBunchOfSources(environment: KotlinCoreEnvironment): Boolean {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        if (!checkKotlinPackageUsage(environment, environment.getSourceFiles())) return false

        val generationState = analyzeAndGenerate(environment) ?: return false

        val mainClass = findMainClass(generationState, environment.getSourceFiles())

        try {
            writeOutput(environment.configuration, generationState.factory, mainClass)
            return true
        }
        finally {
            generationState.destroy()
        }
    }

    internal fun compileAndExecuteScript(environment: KotlinCoreEnvironment, scriptArgs: List<String>): ExitCode {
        val scriptClass = compileScript(environment) ?: return ExitCode.COMPILATION_ERROR

        try {
            try {
                tryConstructClassFromStringArgs(scriptClass, scriptArgs)
                ?: throw RuntimeException("unable to find appropriate constructor for class ${scriptClass.name} accepting arguments $scriptArgs\n")
            }
            finally {
                // NB: these lines are required (see KT-9546) but aren't covered by tests
                System.out.flush()
                System.err.flush()
            }
        }
        catch (e: Throwable) {
            reportExceptionFromScript(e)
            return ExitCode.SCRIPT_EXECUTION_ERROR
        }

        return ExitCode.OK
    }

    private fun repeatAnalysisIfNeeded(
            result: AnalysisResult?,
            environment: KotlinCoreEnvironment,
            targetDescription: String?
    ): AnalysisResult? {
        if (result is AnalysisResult.RetryWithAdditionalJavaRoots) {
            val configuration = environment.configuration

            val oldReadOnlyValue = configuration.isReadOnly
            configuration.isReadOnly = false
            configuration.addJavaSourceRoots(result.additionalJavaRoots)
            configuration.isReadOnly = oldReadOnlyValue

            if (result.addToEnvironment) {
                environment.updateClasspath(result.additionalJavaRoots.map { JavaSourceRoot(it, null) })
            }

            // Clear package caches (see KotlinJavaPsiFacade)
            ApplicationManager.getApplication().runWriteAction {
                (PsiManager.getInstance(environment.project).modificationTracker as? PsiModificationTrackerImpl)?.incCounter()
            }

            // Clear all diagnostic messages
            configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]?.clear()

            // Repeat analysis with additional Java roots (kapt generated sources)
            return analyze(environment, targetDescription)
        }

        return result
    }

    private fun reportExceptionFromScript(exception: Throwable) {
        // expecting InvocationTargetException from constructor invocation with cause that describes the actual cause
        val stream = System.err
        val cause = exception.cause
        if (exception !is InvocationTargetException || cause == null) {
            exception.printStackTrace(stream)
            return
        }
        stream.println(cause)
        val fullTrace = cause.stackTrace
        for (i in 0 until fullTrace.size - exception.stackTrace.size) {
            stream.println("\tat " + fullTrace[i])
        }
    }

    fun compileScript(environment: KotlinCoreEnvironment, parentClassLoader: ClassLoader? = null): Class<*>? {
        val state = analyzeAndGenerate(environment) ?: return null

        try {
            val urls = environment.configuration.getList(JVMConfigurationKeys.CONTENT_ROOTS).mapNotNull { root ->
                when (root) {
                    is JvmModulePathRoot -> root.file // TODO: only add required modules
                    is JvmClasspathRoot -> root.file
                    else -> null
                }
            }.map { it.toURI().toURL() }

            val classLoader = GeneratedClassLoader(state.factory, parentClassLoader ?: URLClassLoader(urls.toTypedArray(), null))

            val script = environment.getSourceFiles()[0].script ?: error("Script must be parsed")
            return classLoader.loadClass(script.fqName.asString())
        }
        catch (e: Exception) {
            throw RuntimeException("Failed to evaluate script: " + e, e)
        }
    }

    fun analyzeAndGenerate(environment: KotlinCoreEnvironment): GenerationState? {
        val result = repeatAnalysisIfNeeded(analyze(environment, null), environment, null) ?: return null

        if (!result.shouldGenerateCode) return null

        result.throwIfError()

        return generate(environment, environment.configuration, result, environment.getSourceFiles(), null)
    }

    private fun analyze(environment: KotlinCoreEnvironment, targetDescription: String?): AnalysisResult? {
        val sourceFiles = environment.getSourceFiles()
        val collector = environment.messageCollector

        val analysisStart = PerformanceCounter.currentTime()
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)
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
                    CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                    environment.configuration,
                    environment::createPackagePartProvider,
                    sourceModuleSearchScope = scope
            )
        }

        val analysisNanos = PerformanceCounter.currentTime() - analysisStart

        val sourceLinesOfCode = environment.countLinesOfCode(sourceFiles)
        val time = TimeUnit.NANOSECONDS.toMillis(analysisNanos)
        val speed = sourceLinesOfCode.toFloat() * 1000 / time

        val message = "ANALYZE: ${sourceFiles.size} files ($sourceLinesOfCode lines) ${targetDescription ?: ""}" +
                      "in $time ms - ${"%.3f".format(speed)} loc/s"

        K2JVMCompiler.reportPerf(environment.configuration, message)

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalJavaRoots)
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

    private fun generate(
            environment: KotlinCoreEnvironment,
            configuration: CompilerConfiguration,
            result: AnalysisResult,
            sourceFiles: List<KtFile>,
            module: Module?
    ): GenerationState {
        val isKapt2Enabled = environment.project.getUserData(IS_KAPT2_ENABLED_KEY) ?: false
        val generationState = GenerationState(
                environment.project,
                ClassBuilderFactories.binaries(isKapt2Enabled),
                result.moduleDescriptor,
                result.bindingContext,
                sourceFiles,
                configuration,
                GenerationState.GenerateClassFilter.GENERATE_ALL,
                if (configuration.getBoolean(JVMConfigurationKeys.IR)) JvmIrCodegenFactory else DefaultCodegenFactory,
                module?.let(::TargetId),
                module?.let(Module::getModuleName),
                module?.let { File(it.getOutputDirectory()) },
                createOutputFilesFlushingCallbackIfPossible(configuration)
        )
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val generationStart = PerformanceCounter.currentTime()

        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

        val generationNanos = PerformanceCounter.currentTime() - generationStart
        val desc = if (module != null) "target " + module.getModuleName() + "-" + module.getModuleType() + " " else ""
        val numberOfSourceFiles = sourceFiles.size
        val numberOfLines = environment.countLinesOfCode(sourceFiles)
        val time = TimeUnit.NANOSECONDS.toMillis(generationNanos)
        val speed = numberOfLines.toFloat() * 1000 / time
        val message = "GENERATE: $numberOfSourceFiles files ($numberOfLines lines) ${desc}in $time ms - ${"%.3f".format(speed)} loc/s"

        K2JVMCompiler.reportPerf(environment.configuration, message)
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        AnalyzerWithCompilerReport.reportDiagnostics(
                FilteredJvmDiagnostics(
                        generationState.collectedExtraJvmDiagnostics,
                        result.bindingContext.diagnostics
                ),
                environment.messageCollector
        )

        AnalyzerWithCompilerReport.reportBytecodeVersionErrors(
                generationState.extraJvmDiagnosticsTrace.bindingContext, environment.messageCollector
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return generationState
    }

    private val KotlinCoreEnvironment.messageCollector: MessageCollector
        get() = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
}
