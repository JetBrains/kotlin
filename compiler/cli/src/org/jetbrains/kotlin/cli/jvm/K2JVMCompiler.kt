/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.Companion.VERBOSE
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.common.profiling.ProfilingCompilerPerformanceManager
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.util.PhaseMeasurementType
import java.io.File

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun shouldRunK2(
        messageCollector: MessageCollector,
        arguments: K2JVMCompilerArguments,
    ): Boolean {
        val isK2 = super.shouldRunK2(messageCollector, arguments)
        if (kaptIsEnabled(arguments)) {
            if (isK2 && arguments.useK2Kapt == false) {
                arguments.languageVersion = LanguageVersion.KOTLIN_1_9.versionString
                if (arguments.apiVersion?.startsWith("2") == true) {
                    arguments.apiVersion = ApiVersion.KOTLIN_1_9.versionString
                }
                arguments.skipMetadataVersionCheck = true
                arguments.skipPrereleaseCheck = true
                arguments.allowUnstableDependencies = true
                return false
            }
            if (!isK2 && arguments.useK2Kapt == true) {
                messageCollector.report(STRONG_WARNING, "K2 kapt cannot be enabled in K1. Update language version to 2.0 or newer.")
                return false
            }
        }

        return isK2
    }

    override fun doExecutePhased(
        arguments: K2JVMCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        return JvmCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    override fun doExecute(
        arguments: K2JVMCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        configuration.phaseConfig = createPhaseConfig(arguments, jvmPhases).also {
            if (arguments.listPhases) it.list(jvmPhases)
        }

        if (!configuration.configureJdkHome(arguments)) return COMPILATION_ERROR

        configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, arguments.disableStandardScript)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration, rootDisposable)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        configuration.configureJavaModulesContentRoots(arguments)
        configuration.configureStandardLibs(paths, arguments)
        configuration.configureAdvancedJvmOptions(arguments)
        configuration.configureKlibPaths(arguments)

        if (
            arguments.buildFile == null &&
            !arguments.version &&
            !arguments.allowNoSourceFiles &&
            (arguments.script || arguments.expression != null || arguments.freeArgs.isEmpty())
        ) {
            configuration.configureContentRootsFromClassPath(arguments)
            configuration.configureJdkClasspathRoots()

            // script or repl
            if (arguments.script && arguments.freeArgs.isEmpty()) {
                messageCollector.report(ERROR, "Specify script source path to evaluate")
                return COMPILATION_ERROR
            }

            val projectEnvironment =
                KotlinCoreEnvironment.ProjectEnvironment(
                    rootDisposable,
                    KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(rootDisposable, configuration),
                    configuration
                )
            projectEnvironment.registerExtensionsFromPlugins(configuration)

            if (arguments.script || arguments.expression != null) {
                val scriptingEvaluator = ScriptEvaluationExtension.getInstances(projectEnvironment.project).find { it.isAccepted(arguments) }
                if (scriptingEvaluator == null) {
                    messageCollector.report(ERROR, "Unable to evaluate script, no scripting plugin loaded")
                    return COMPILATION_ERROR
                }
                return scriptingEvaluator.eval(arguments, configuration, projectEnvironment)
            } else {
                val shell = ShellExtension.getInstances(projectEnvironment.project).find { it.isAccepted(arguments) }
                if (shell == null) {
                    messageCollector.report(ERROR, "Unable to run REPL, no scripting plugin loaded")
                    return COMPILATION_ERROR
                }
                return shell.run(arguments, configuration, projectEnvironment)
            }
        }

        messageCollector.report(LOGGING, "Configuring the compilation environment")
        try {
            val buildFile = arguments.buildFile?.let { File(it) }

            val moduleChunk = configuration.configureModuleChunk(arguments, buildFile)

            val chunk = moduleChunk.modules
            configuration.configureSourceRoots(chunk, buildFile)
            // should be called after configuring jdk home from build file
            configuration.configureJdkClasspathRoots()

            val environment = createCoreEnvironment(
                rootDisposable, configuration, messageCollector,
                moduleChunk.targetDescription()
            ) ?: run {
                configuration.perfManager?.notifyPhaseFinished(PhaseMeasurementType.Initialization)
                return COMPILATION_ERROR
            }
            environment.registerJavacIfNeeded(arguments).let {
                if (!it) return COMPILATION_ERROR
            }

            if (environment.getSourceFiles().isEmpty() && !arguments.allowNoSourceFiles && buildFile == null) {
                if (arguments.version) return OK

                messageCollector.report(ERROR, "No source files")
                return COMPILATION_ERROR
            }

            if (!KotlinToJVMBytecodeCompiler.compileModules(environment, buildFile, chunk)) return COMPILATION_ERROR
            return OK
        } catch (e: CompilationException) {
            messageCollector.report(
                EXCEPTION,
                OutputMessageUtil.renderException(e),
                MessageUtil.psiElementToMessageLocation(e.element)
            )
            return INTERNAL_ERROR
        }
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JVMCompilerArguments) {
        if (arguments.scriptTemplates?.isNotEmpty() == true) {
            add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates!!.joinToString(",")}")
        }
        if (arguments.scriptResolverEnvironment?.isNotEmpty() == true) {
            add(
                "plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment!!.joinToString(
                    ","
                )}"
            )
        }
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2JVMCompilerArguments, services: Services
    ) {
        with(configuration) {
            if (incrementalCompilationIsEnabled(arguments)) {
                putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, services[LookupTracker::class.java])

                putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, services[ExpectActualTracker::class.java])

                putIfNotNull(CommonConfigurationKeys.INLINE_CONST_TRACKER, services[InlineConstTracker::class.java])

                putIfNotNull(CommonConfigurationKeys.ENUM_WHEN_TRACKER, services[EnumWhenTracker::class.java])

                putIfNotNull(CommonConfigurationKeys.IMPORT_TRACKER, services[ImportTracker::class.java])

                putIfNotNull(
                    JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS,
                    services[IncrementalCompilationComponents::class.java]
                )

                putIfNotNull(ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER, services[JavaClassesTracker::class.java])
            }
            setupJvmSpecificArguments(arguments)
        }
    }

    override fun createArguments(): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
        if (System.getenv("KOTLIN_REPORT_PERF") != null) {
            reportPerf = true
        }
    }

    override fun executableScriptFileName(): String = "kotlinc-jvm"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JVMCompiler(), args)
        }

        fun createCoreEnvironment(
            rootDisposable: Disposable,
            configuration: CompilerConfiguration,
            messageCollector: MessageCollector,
            targetDescription: String
        ): KotlinCoreEnvironment? {
            val perfManager = configuration.perfManager
            perfManager?.targetDescription = targetDescription

            if (messageCollector.hasErrors()) return null

            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            val sourceFiles = environment.getSourceFiles()
            perfManager?.addSourcesStats(sourceFiles.size, environment.countLinesOfCode(sourceFiles))

            return if (messageCollector.hasErrors()) null else environment
        }

        internal fun kaptIsEnabled(arguments: K2JVMCompilerArguments): Boolean {
            return arguments.pluginOptions?.any { it.startsWith("plugin:org.jetbrains.kotlin.kapt3") } == true
        }

        internal fun createCustomPerformanceManagerOrNull(
            arguments: K2JVMCompilerArguments,
            services: Services,
        ): PerformanceManager? {
            val externalManager = services[PerformanceManager::class.java]
            if (externalManager != null) return externalManager
            val argument = arguments.profileCompilerCommand ?: return null
            return ProfilingCompilerPerformanceManager.create(argument)
        }
    }

    override fun createPerformanceManager(arguments: K2JVMCompilerArguments, services: Services): PerformanceManager {
        return createCustomPerformanceManagerOrNull(arguments, services) ?: defaultPerformanceManager
    }
}

fun CompilerConfiguration.configureModuleChunk(
    arguments: K2JVMCompilerArguments,
    buildFile: File?
): ModuleChunk {
    val destination = arguments.destination?.let { File(it) }

    return if (buildFile != null) {
        val messageCollector = getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        fun strongWarning(message: String) {
            messageCollector.report(STRONG_WARNING, message)
        }

        if (destination != null) {
            strongWarning("The '-d' option with a directory destination is ignored because '-Xbuild-file' is specified")
        }
        if (arguments.javaSourceRoots != null) {
            strongWarning("The '-Xjava-source-roots' option is ignored because '-Xbuild-file' is specified")
        }
        if (arguments.javaPackagePrefix != null) {
            strongWarning("The '-Xjava-package-prefix' option is ignored because '-Xbuild-file' is specified")
        }
        configureContentRootsFromClassPath(arguments)
        val sanitizedCollector = FilteringMessageCollector(messageCollector, VERBOSE::contains)
        put(JVMConfigurationKeys.MODULE_XML_FILE, buildFile)
        CompileEnvironmentUtil.loadModuleChunk(buildFile, sanitizedCollector)
    } else {
        if (destination != null) {
            if (destination.path.endsWith(".jar")) {
                put(JVMConfigurationKeys.OUTPUT_JAR, destination)
            } else {
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destination)
            }
        }

        val module = ModuleBuilder(
            this[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
            destination?.path ?: ".", "java-production"
        )
        module.configureFromArgs(arguments)

        ModuleChunk(listOf(module))
    }
}

internal fun ModuleChunk.targetDescription(): String {
    return modules
        .map { input -> input.getModuleName() + "-" + input.getModuleType() }
        .let { names -> names.singleOrNull() ?: names.joinToString() }
}

fun main(args: Array<String>) = K2JVMCompiler.main(args)
