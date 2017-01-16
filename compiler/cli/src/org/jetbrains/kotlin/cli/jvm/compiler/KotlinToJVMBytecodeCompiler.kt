/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.util.io.JarUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAll
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes

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
            outputDir: File?,
            jarPath: File?,
            jarRuntime: Boolean,
            mainClass: FqName?
    ) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        if (jarPath != null) {
            CompileEnvironmentUtil.writeToJar(jarPath, jarRuntime, mainClass, outputFiles)
            messageCollector.report(CompilerMessageSeverity.OUTPUT,
                                    OutputMessageUtil.formatOutputMessage(outputFiles.asList().flatMap { it.sourceFiles }.distinct(), jarPath), CompilerMessageLocation.NO_LOCATION)
        }
        else {
            outputFiles.writeAll(outputDir ?: File("."), messageCollector)
        }
    }

    private fun createOutputFilesFlushingCallbackIfPossible(
            configuration: CompilerConfiguration,
            outputDir: File?,
            jarPath: File?
    ): GenerationStateEventCallback {
        if (jarPath != null) return GenerationStateEventCallback.DO_NOTHING

        return GenerationStateEventCallback { state ->
            val currentOutput = SimpleOutputFileCollection(state.factory.currentOutput)
            writeOutput(configuration, currentOutput, outputDir, jarPath = null, jarRuntime = false, mainClass = null)
            state.factory.releaseGeneratedOutput()
        }
    }

    fun compileModules(
            environment: KotlinCoreEnvironment,
            configuration: CompilerConfiguration,
            chunk: List<Module>,
            directory: File,
            jarPath: File?,
            friendPaths: List<String>,
            jarRuntime: Boolean
    ): Boolean {
        val outputFiles = hashMapOf<Module, ClassFileFactory>()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        val targetDescription = "in targets [" + chunk.joinToString { input -> input.getModuleName() + "-" + input.getModuleType() } + "]"

        val result = repeatAnalysisIfNeeded(analyze(environment, targetDescription), environment, targetDescription)
        if (result == null || !result.shouldGenerateCode) return false

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val generationStates = ArrayList<GenerationState>();

        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            val ktFiles = CompileEnvironmentUtil.getKtFiles(
                    environment.project, getAbsolutePaths(directory, module), configuration
            ) { path -> throw IllegalStateException("Should have been checked before: $path") }
            if (!checkKotlinPackageUsage(environment, ktFiles)) return false
            val moduleOutputDirectory = File(module.getOutputDirectory())

            val onIndependentPartCompilationEnd =
                    createOutputFilesFlushingCallbackIfPossible(configuration, File(module.getOutputDirectory()), jarPath)

            val generationState = generate(environment, result, ktFiles, module, moduleOutputDirectory,
                                           module.getModuleName(),  onIndependentPartCompilationEnd)

            outputFiles.put(module, generationState.factory)
            generationStates.add(generationState);
        }

        try {
            for (module in chunk) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                writeOutput(configuration, outputFiles[module]!!, File(module.getOutputDirectory()), jarPath, jarRuntime, null)
            }
            return true
        }
        finally {
            for (generationState in generationStates) {
                generationState.destroy();
            }
        }
    }

    fun createCompilerConfiguration(
            base: CompilerConfiguration,
            chunk: List<Module>,
            directory: File
    ): CompilerConfiguration {
        val configuration = base.copy()

        for (module in chunk) {
            configuration.addKotlinSourceRoots(getAbsolutePaths(directory, module))
        }

        for (module in chunk) {
            for (javaRootPath in module.getJavaSourceRoots()) {
                configuration.addJavaSourceRoot(File(javaRootPath.path), javaRootPath.packagePrefix)
            }
        }

        for (module in chunk) {
            for (classpathRoot in module.getClasspathRoots()) {
                configuration.addJvmClasspathRoot(File(classpathRoot))
            }
        }

        configuration.addAll(JVMConfigurationKeys.MODULES, chunk)

        return configuration
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

    fun compileBunchOfSources(
            environment: KotlinCoreEnvironment,
            jar: File?,
            outputDir: File?,
            friendPaths: List<String>,
            includeRuntime: Boolean
    ): Boolean {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        if (!checkKotlinPackageUsage(environment, environment.getSourceFiles())) return false

        val onIndependentPartCompilationEnd = createOutputFilesFlushingCallbackIfPossible(environment.configuration, outputDir, jar)
        val generationState = analyzeAndGenerate(environment, onIndependentPartCompilationEnd) ?: return false

        val mainClass = findMainClass(generationState, environment.getSourceFiles())

        try {
            writeOutput(environment.configuration, generationState.factory, outputDir, jar, includeRuntime, mainClass)
            return true
        }
        finally {
            generationState.destroy()
        }
    }

    fun compileAndExecuteScript(
            configuration: CompilerConfiguration,
            paths: KotlinPaths,
            environment: KotlinCoreEnvironment,
            scriptArgs: List<String>
    ): ExitCode {
        val scriptClass = compileScript(configuration, paths, environment) ?: return ExitCode.COMPILATION_ERROR
        val scriptConstructor = getScriptConstructor(scriptClass)

        try {
            try {
                scriptConstructor.newInstance(*arrayOf<Any>(scriptArgs.toTypedArray()))
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
                environment.addJavaSourceRoots(result.additionalJavaRoots.map { JavaSourceRoot(it, null) })
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
        val relevantEntries = fullTrace.size - exception.stackTrace.size
        for (i in 0..relevantEntries - 1) {
            stream.println("\tat " + fullTrace[i])
        }
    }

    private fun getScriptConstructor(scriptClass: Class<*>): Constructor<*> =
            scriptClass.getConstructor(Array<String>::class.java)

    fun compileScript(
            configuration: CompilerConfiguration,
            paths: KotlinPaths,
            environment: KotlinCoreEnvironment
    ): Class<*>? {
        val state = analyzeAndGenerate(environment, GenerationStateEventCallback.DO_NOTHING) ?: return null

        val classLoader: GeneratedClassLoader
        try {
            val classPaths = arrayListOf(paths.runtimePath.toURI().toURL())
            configuration.jvmClasspathRoots.mapTo(classPaths) { it.toURI().toURL() }
            classLoader = GeneratedClassLoader(state.factory, URLClassLoader(classPaths.toTypedArray(), null))

            val script = environment.getSourceFiles()[0].script ?: error("Script must be parsed")
            return classLoader.loadClass(script.fqName.asString())
        }
        catch (e: Exception) {
            throw RuntimeException("Failed to evaluate script: " + e, e)
        }
    }

    fun analyzeAndGenerate(
            environment: KotlinCoreEnvironment,
            onIndependentPartCompilationEnd: GenerationStateEventCallback
    ): GenerationState? {
        val result = repeatAnalysisIfNeeded(analyze(environment, null), environment, null) ?: return null

        if (!result.shouldGenerateCode) return null

        result.throwIfError()

        return generate(environment, result, environment.getSourceFiles(), null, null, null, onIndependentPartCompilationEnd)
    }

    private fun analyze(environment: KotlinCoreEnvironment, targetDescription: String?): AnalysisResult? {
        val collector = environment.messageCollector

        val analysisStart = PerformanceCounter.currentTime()
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult {
                val sharedTrace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
                val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project,
                                                                                              environment.getModuleName())

                return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                        moduleContext,
                        environment.getSourceFiles(),
                        sharedTrace,
                        environment.configuration.get(JVMConfigurationKeys.MODULES),
                        environment.configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                        JvmPackagePartProvider(environment))
            }

            override fun reportEnvironmentErrors() {
                reportRuntimeConflicts(collector, environment.configuration.jvmClasspathRoots)
            }
        })

        val analysisNanos = PerformanceCounter.currentTime() - analysisStart

        val sourceLinesOfCode = environment.sourceLinesOfCode
        val numberOfFiles = environment.getSourceFiles().size
        val time = TimeUnit.NANOSECONDS.toMillis(analysisNanos)
        val speed = sourceLinesOfCode.toFloat() * 1000 / time

        val message = "ANALYZE: $numberOfFiles files ($sourceLinesOfCode lines) ${targetDescription ?: ""}" +
                      "in $time ms - ${"%.3f".format(speed)} loc/s"

        K2JVMCompiler.reportPerf(environment.configuration, message)

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalJavaRoots)
            analysisResult
        else
            null
    }

    private fun generate(
            environment: KotlinCoreEnvironment,
            result: AnalysisResult,
            sourceFiles: List<KtFile>,
            module: Module?,
            outputDirectory: File?,
            moduleName: String?,
            onIndependentPartCompilationEnd: GenerationStateEventCallback
    ): GenerationState {
        val configuration = environment.configuration
        val incrementalCompilationComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)

        val packagesWithObsoleteParts = hashSetOf<FqName>()
        val obsoleteMultifileClasses = arrayListOf<FqName>()
        var targetId: TargetId? = null

        if (module != null && incrementalCompilationComponents != null) {
            targetId = TargetId(module)
            val incrementalCache = incrementalCompilationComponents.getIncrementalCache(targetId)

            for (internalName in incrementalCache.getObsoletePackageParts()) {
                packagesWithObsoleteParts.add(JvmClassName.byInternalName(internalName).packageFqName)
            }

            for (obsoleteFacadeInternalName in incrementalCache.getObsoleteMultifileClasses()) {
                obsoleteMultifileClasses.add(JvmClassName.byInternalName(obsoleteFacadeInternalName).fqNameForClassNameWithoutDollars)
            }
        }

        val isKapt2Enabled = environment.project.getUserData(IS_KAPT2_ENABLED_KEY) ?: false
        val generationState = GenerationState(
                environment.project,
                ClassBuilderFactories.binaries(isKapt2Enabled),
                result.moduleDescriptor,
                result.bindingContext,
                sourceFiles,
                configuration.getBoolean(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS),
                configuration.getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS),
                GenerationState.GenerateClassFilter.GENERATE_ALL,
                configuration.getBoolean(JVMConfigurationKeys.DISABLE_INLINE),
                configuration.getBoolean(JVMConfigurationKeys.DISABLE_OPTIMIZATION),
                /* useTypeTableInSerializer = */ false,
                configuration.getBoolean(JVMConfigurationKeys.INHERIT_MULTIFILE_PARTS),
                packagesWithObsoleteParts,
                obsoleteMultifileClasses,
                targetId,
                moduleName,
                outputDirectory,
                incrementalCompilationComponents,
                onIndependentPartCompilationEnd = onIndependentPartCompilationEnd,
                dumpBinarySignatureMappingTo = configuration.get(JVMConfigurationKeys.DECLARATIONS_JSON_PATH)?.let { File(it) }
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

    private fun checkKotlinPackageUsage(environment: KotlinCoreEnvironment, files: Collection<KtFile>): Boolean {
        if (environment.configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)) {
            return true
        }
        val messageCollector = environment.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val kotlinPackage = FqName.topLevel(Name.identifier("kotlin"))
        files.forEach {
            if (it.packageFqName.isSubpackageOf(kotlinPackage)) {
                messageCollector.report(CompilerMessageSeverity.ERROR,
                                        "Only the Kotlin standard library is allowed to use the 'kotlin' package",
                                        MessageUtil.psiElementToMessageLocation(it.packageDirective!!))
                return false
            }
        }
        return true
    }

    private val KotlinCoreEnvironment.messageCollector: MessageCollector
        get() = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    private fun reportRuntimeConflicts(messageCollector: MessageCollector, jvmClasspathRoots: List<File>) {
        fun String.removeIdeaVersionSuffix(): String {
            val versionIndex = indexOfAny(arrayListOf("-IJ", "-Idea"))
            return if (versionIndex >= 0) substring(0, versionIndex) else this
        }

        val runtimes = jvmClasspathRoots.map {
            try {
                it.canonicalFile
            }
            catch (e: IOException) {
                it
            }
        }.filter { it.name == PathUtil.KOTLIN_JAVA_RUNTIME_JAR && it.exists() }

        val runtimeVersions = runtimes.map {
            JarUtil.getJarAttribute(it, Attributes.Name.IMPLEMENTATION_VERSION).orEmpty().removeIdeaVersionSuffix()
        }

        if (runtimeVersions.toSet().size > 1) {
            messageCollector.report(CompilerMessageSeverity.ERROR,
                                    "Conflicting versions of Kotlin runtime on classpath: " + runtimes.joinToString { it.path },
                                    CompilerMessageLocation.NO_LOCATION)
        }
    }
}