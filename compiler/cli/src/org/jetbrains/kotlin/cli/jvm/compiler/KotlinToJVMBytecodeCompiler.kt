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

import com.intellij.openapi.util.io.JarUtil
import org.jetbrains.annotations.TestOnly
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
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
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
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.defaultType
import kotlin.reflect.jvm.javaType

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
        val jarPath = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
        if (jarPath != null) {
            val includeRuntime = configuration.get(JVMConfigurationKeys.INCLUDE_RUNTIME, false)
            CompileEnvironmentUtil.writeToJar(jarPath, includeRuntime, mainClass, outputFiles)
            return
        }

        val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) ?: File(".")
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        outputFiles.writeAll(outputDir, messageCollector)
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
            for ((module, state) in outputs) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                writeOutput(state.configuration, state.factory, null)
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

    fun compileAndExecuteScript(
            environment: KotlinCoreEnvironment,
            paths: KotlinPaths,
            scriptArgs: List<String>): ExitCode
    {
        val scriptClass = compileScript(environment, paths) ?: return ExitCode.COMPILATION_ERROR

        try {
            try {
                tryConstructClass(scriptClass, scriptArgs)
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
            (PsiManager.getInstance(environment.project).modificationTracker as? PsiModificationTrackerImpl)?.incCounter()

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

    @TestOnly
    fun tryConstructClassPub(scriptClass: Class<*>, scriptArgs: List<String>): Any? = tryConstructClass(scriptClass, scriptArgs)

    private fun tryConstructClass(scriptClass: Class<*>, scriptArgs: List<String>): Any? {

        fun convertPrimitive(type: KType?, arg: String): Any? =
                when (type) {
                    String::class.defaultType -> arg
                    Int::class.defaultType -> arg.toInt()
                    Long::class.defaultType -> arg.toLong()
                    Short::class.defaultType -> arg.toShort()
                    Byte::class.defaultType -> arg.toByte()
                    Char::class.defaultType -> arg[0]
                    Float::class.defaultType -> arg.toFloat()
                    Double::class.defaultType -> arg.toDouble()
                    Boolean::class.defaultType -> arg.toBoolean()
                    else -> null
                }

        fun convertArray(type: KType?, args: List<String>): Any? =
                when (type) {
                    String::class.defaultType -> args.toTypedArray()
                    Int::class.defaultType -> args.map { it.toInt() }.toTypedArray()
                    Long::class.defaultType -> args.map { it.toLong() }.toTypedArray()
                    Short::class.defaultType -> args.map { it.toShort() }.toTypedArray()
                    Byte::class.defaultType -> args.map { it.toByte() }.toTypedArray()
                    Char::class.defaultType -> args.map { it[0] }.toTypedArray()
                    Float::class.defaultType -> args.map { it.toFloat() }.toTypedArray()
                    Double::class.defaultType -> args.map { it.toDouble() }.toTypedArray()
                    Boolean::class.defaultType -> args.map { it.toBoolean() }.toTypedArray()
                    else -> null
                }

        fun foldingFunc(state: Pair<List<Any>, List<String>?>, par: KParameter): Pair<List<Any>, List<String>?> {
            state.second?.let { scriptArgsLeft ->
                try {
                    if (scriptArgsLeft.isNotEmpty()) {
                        val primArgCandidate = convertPrimitive(par.type, scriptArgsLeft.first())
                        if (primArgCandidate != null)
                            return@foldingFunc Pair(state.first + primArgCandidate, scriptArgsLeft.drop(1))
                    }

                    val arrCompType = (par.type.javaType as? Class<*>)?.componentType?.kotlin?.defaultType
                    val arrayArgCandidate = convertArray(arrCompType, scriptArgsLeft)
                    if (arrayArgCandidate != null)
                        return@foldingFunc Pair(state.first + arrayArgCandidate, null)
                }
                catch (e: NumberFormatException) {
                } // just skips to return below
            }
            return state
        }

        try {
            return scriptClass.getConstructor(Array<String>::class.java).newInstance(*arrayOf<Any>(scriptArgs.toTypedArray()))
        }
        catch (e: java.lang.NoSuchMethodException) {
            for (ctor in scriptClass.kotlin.constructors) {
                val (ctorArgs, scriptArgsLeft) = ctor.parameters.fold(Pair(emptyList<Any>(), scriptArgs), ::foldingFunc)
                if (ctorArgs.size <= ctor.parameters.size && (scriptArgsLeft == null || scriptArgsLeft.isEmpty())) {
                    val argsMap = ctorArgs.zip(ctor.parameters) { a, p -> Pair(p, a) }.toMap()
                    try {
                        return ctor.callBy(argsMap)
                    }
                    catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
                    }
                }
            }
        }
        return null
    }

    fun compileScript(environment: KotlinCoreEnvironment, paths: KotlinPaths): Class<*>? =
            compileScript(environment,
                          {
                              val classPaths = arrayListOf(paths.runtimePath.toURI().toURL())
                              environment.configuration.jvmClasspathRoots.mapTo(classPaths) { it.toURI().toURL() }
                              URLClassLoader(classPaths.toTypedArray())
                          })

    fun compileScript(environment: KotlinCoreEnvironment, parentClassLoader: ClassLoader): Class<*>? = compileScript(environment, { parentClassLoader })

    private inline fun compileScript(
            environment: KotlinCoreEnvironment,
            makeParentClassLoader: () -> ClassLoader): Class<*>? {
        val state = analyzeAndGenerate(environment) ?: return null

        try {
            val classLoader = GeneratedClassLoader(state.factory, makeParentClassLoader())

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
        val collector = environment.messageCollector

        val analysisStart = PerformanceCounter.currentTime()
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector)
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult {
                val sharedTrace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
                val moduleContext =
                        TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)

                return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                        moduleContext,
                        environment.getSourceFiles(),
                        sharedTrace,
                        environment.configuration,
                        JvmPackagePartProvider(environment)
                )
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
                module?.let(::TargetId),
                module?.let { it.getModuleName() },
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
