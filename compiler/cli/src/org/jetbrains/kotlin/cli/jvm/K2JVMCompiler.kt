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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.common.profiling.ProfilingCompilerPerformanceManager
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileModulesUsingFrontendIrAndLightTree
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {

    override fun doExecute(
        arguments: K2JVMCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        configuration.putIfNotNull(CLIConfigurationKeys.REPEAT_COMPILE_MODULES, arguments.repeatCompileModules?.toIntOrNull())
        configuration.put(CLIConfigurationKeys.PHASE_CONFIG, createPhaseConfig(jvmPhases, arguments, messageCollector))

        if (!configuration.configureJdkHome(arguments)) return COMPILATION_ERROR

        configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, arguments.disableStandardScript)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
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

            if (arguments.useOldBackend) {
                messageCollector.report(WARNING, "-Xuse-old-backend is no longer supported. Please migrate to the new JVM IR backend")
            }

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

        if (arguments.useOldBackend) {
            val severity = if (isUseOldBackendAllowed()) WARNING else ERROR
            messageCollector.report(severity, "-Xuse-old-backend is no longer supported. Please migrate to the new JVM IR backend")
            if (severity == ERROR) return COMPILATION_ERROR
        }

        messageCollector.report(LOGGING, "Configuring the compilation environment")
        try {
            val buildFile = arguments.buildFile?.let { File(it) }

            val moduleChunk = configuration.configureModuleChunk(arguments, buildFile)

            val chunk = moduleChunk.modules
            configuration.configureSourceRoots(chunk, buildFile)
            // should be called after configuring jdk home from build file
            configuration.configureJdkClasspathRoots()

            val targetDescription = chunk.map { input -> input.getModuleName() + "-" + input.getModuleType() }.let { names ->
                names.singleOrNull() ?: names.joinToString()
            }
            if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR) && arguments.useFirLT /* TODO: consider storing in the configuration instead of using args here directly */) {
                val projectEnvironment =
                    createProjectEnvironment(configuration, rootDisposable, EnvironmentConfigFiles.JVM_CONFIG_FILES, messageCollector)

                compileModulesUsingFrontendIrAndLightTree(
                    projectEnvironment, configuration, messageCollector, buildFile, chunk, targetDescription
                )
            } else {
                val environment = createCoreEnvironment(
                    rootDisposable, configuration, messageCollector,
                    targetDescription
                ) ?: return COMPILATION_ERROR
                environment.registerJavacIfNeeded(arguments).let {
                    if (!it) return COMPILATION_ERROR
                }

                if (environment.getSourceFiles().isEmpty() && !arguments.allowNoSourceFiles && buildFile == null) {
                    if (arguments.version) return OK

                    messageCollector.report(ERROR, "No source files")
                    return COMPILATION_ERROR
                }

                KotlinToJVMBytecodeCompiler.compileModules(environment, buildFile, chunk)
            }
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

    private fun createCoreEnvironment(
        rootDisposable: Disposable,
        configuration: CompilerConfiguration,
        messageCollector: MessageCollector,
        targetDescription: String
    ): KotlinCoreEnvironment? {
        if (messageCollector.hasErrors()) return null

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val sourceFiles = environment.getSourceFiles()
        configuration[CLIConfigurationKeys.PERF_MANAGER]?.notifyCompilerInitialized(
            sourceFiles.size, environment.countLinesOfCode(sourceFiles), targetDescription
        )

        return if (messageCollector.hasErrors()) null else environment
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

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = JvmMetadataVersion(*versionArray)

    protected class K2JVMCompilerPerformanceManager : CommonCompilerPerformanceManager("Kotlin to JVM Compiler")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CLITool.doMain(K2JVMCompiler(), args)
        }

    }

    override val defaultPerformanceManager: CommonCompilerPerformanceManager = K2JVMCompilerPerformanceManager()

    override fun createPerformanceManager(arguments: K2JVMCompilerArguments, services: Services): CommonCompilerPerformanceManager {
        val externalManager = services[CommonCompilerPerformanceManager::class.java]
        if (externalManager != null) return externalManager
        val argument = arguments.profileCompilerCommand ?: return defaultPerformanceManager
        return ProfilingCompilerPerformanceManager.create(argument)
    }

    private fun isUseOldBackendAllowed(): Boolean =
        K2JVMCompiler::class.java.classLoader.getResource("META-INF/unsafe-allow-use-old-backend") != null
}

fun CompilerConfiguration.configureModuleChunk(
    arguments: K2JVMCompilerArguments,
    buildFile: File?
): ModuleChunk {
    val destination = arguments.destination?.let { File(it) }

    return if (buildFile != null) {
        val messageCollector = getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

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


fun main(args: Array<String>) = K2JVMCompiler.main(args)
