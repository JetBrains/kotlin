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

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.jvm.repl.ReplFromTerminal
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {

    override val performanceManager = K2JVMCompilerPerformanceManager()

    override fun doExecute(
        arguments: K2JVMCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        if (!configuration.configureJdkHome(arguments)) return COMPILATION_ERROR

        configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, arguments.disableStandardScript)

        val pluginLoadResult = loadPlugins(arguments, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val commonSources = arguments.commonSources?.toSet().orEmpty()
        if (!arguments.script && arguments.buildFile == null) {
            for (arg in arguments.freeArgs) {
                val file = File(arg)
                if (file.extension == JavaFileType.DEFAULT_EXTENSION) {
                    configuration.addJavaSourceRoot(file)
                } else {
                    configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources)
                    if (file.isDirectory) {
                        configuration.addJavaSourceRoot(file)
                    }
                }
            }
        }

        configuration.put(CommonConfigurationKeys.MODULE_NAME, arguments.moduleName ?: JvmAbi.DEFAULT_MODULE_NAME)

        configuration.configureExplicitContentRoots(arguments)
        configuration.configureStandardLibs(paths, arguments)

        if (arguments.buildFile == null && arguments.freeArgs.isEmpty() && !arguments.version) {
            if (arguments.script) {
                messageCollector.report(ERROR, "Specify script source path to evaluate")
                return COMPILATION_ERROR
            }
            ReplFromTerminal.run(rootDisposable, configuration)
            return ExitCode.OK
        }

        configuration.configureAdvancedJvmOptions(arguments)

        messageCollector.report(LOGGING, "Configuring the compilation environment")
        try {
            val destination = arguments.destination

            if (arguments.buildFile != null) {
                if (destination != null) {
                    messageCollector.report(
                        STRONG_WARNING,
                        "The '-d' option with a directory destination is ignored because '-Xbuild-file' is specified"
                    )
                }

                val sanitizedCollector = FilteringMessageCollector(messageCollector, VERBOSE::contains)
                val buildFile = File(arguments.buildFile)
                val moduleChunk = CompileEnvironmentUtil.loadModuleChunk(buildFile, sanitizedCollector)

                configuration.put(JVMConfigurationKeys.MODULE_XML_FILE, buildFile)

                KotlinToJVMBytecodeCompiler.configureSourceRoots(configuration, moduleChunk.modules, buildFile)

                val environment = createCoreEnvironment(rootDisposable, configuration, messageCollector)
                    ?: return COMPILATION_ERROR

                environment.registerJavacIfNeeded(arguments).let {
                    if (!it) return COMPILATION_ERROR
                }

                KotlinToJVMBytecodeCompiler.compileModules(environment, buildFile, moduleChunk.modules)
            } else if (arguments.script) {
                val sourcePath = arguments.freeArgs.first()
                configuration.addKotlinSourceRoot(sourcePath)

                configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

                val environment = createCoreEnvironment(rootDisposable, configuration, messageCollector)
                    ?: return COMPILATION_ERROR

                val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(environment.project)
                val scriptFile = File(sourcePath)
                if (scriptFile.isDirectory || !scriptDefinitionProvider.isScript(scriptFile.name)) {
                    val extensionHint =
                        if (configuration.get(JVMConfigurationKeys.SCRIPT_DEFINITIONS) == listOf(StandardScriptDefinition)) " (.kts)"
                        else ""
                    messageCollector.report(ERROR, "Specify path to the script file$extensionHint as the first argument")
                    return COMPILATION_ERROR
                }

                val scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size)
                return KotlinToJVMBytecodeCompiler.compileAndExecuteScript(environment, scriptArgs)
            } else {
                if (destination != null) {
                    if (destination.endsWith(".jar")) {
                        configuration.put(JVMConfigurationKeys.OUTPUT_JAR, File(destination))
                    } else {
                        configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(destination))
                    }
                }

                val environment = createCoreEnvironment(rootDisposable, configuration, messageCollector)
                    ?: return COMPILATION_ERROR

                environment.registerJavacIfNeeded(arguments).let {
                    if (!it) return COMPILATION_ERROR
                }

                if (environment.getSourceFiles().isEmpty()) {
                    if (arguments.version) {
                        return OK
                    }
                    messageCollector.report(ERROR, "No source files")
                    return COMPILATION_ERROR
                }

                KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment)

                compileJavaFilesIfNeeded(environment, arguments).let {
                    if (!it) return COMPILATION_ERROR
                }
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

    private fun loadPlugins(arguments: K2JVMCompilerArguments, configuration: CompilerConfiguration): ExitCode {
        var pluginClasspaths: Iterable<String> = arguments.pluginClasspaths?.asIterable() ?: emptyList()
        val pluginOptions = arguments.pluginOptions?.toMutableList() ?: ArrayList()

        if (!arguments.disableDefaultScriptingPlugin) {
            val explicitOrLoadedScriptingPlugin =
                pluginClasspaths.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) } ||
                        try {
                            PluginCliParser::class.java.classLoader.loadClass("org.jetbrains.kotlin.extensions.ScriptingCompilerConfigurationExtension")
                            true
                        } catch (_: Throwable) {
                            false
                        }
            // if scripting plugin is not enabled explicitly (probably from another path) and not in the classpath already,
            // try to find and enable it implicitly
            if (!explicitOrLoadedScriptingPlugin) {
                val libPath = PathUtil.kotlinPathsForCompiler.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
                with(PathUtil) {
                    val jars = arrayOf(
                        KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR, KOTLIN_SCRIPTING_COMMON_JAR,
                        KOTLIN_SCRIPTING_JVM_JAR
                    ).mapNotNull { File(libPath, it).takeIf { it.exists() }?.canonicalPath }
                    if (jars.size == 3) {
                        pluginClasspaths = jars + pluginClasspaths
                    }
                }
            }
            if (arguments.scriptTemplates?.isNotEmpty() == true) {
                pluginOptions.add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates!!.joinToString(",")}")
            }
            if (arguments.scriptResolverEnvironment?.isNotEmpty() == true) {
                pluginOptions.add(
                    "plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment!!.joinToString(
                        ","
                    )}"
                )
            }
        } else {
            pluginOptions.add("plugin:kotlin.scripting:disable=true")
        }
        return PluginCliParser.loadPluginsSafe(pluginClasspaths, pluginOptions, configuration)
    }

    private fun compileJavaFilesIfNeeded(
        environment: KotlinCoreEnvironment,
        arguments: K2JVMCompilerArguments
    ): Boolean {
        if (arguments.compileJava) {
            return JavacWrapper.getInstance(environment.project).use { it.compile() }
        }
        return true
    }

    private fun createCoreEnvironment(
        rootDisposable: Disposable,
        configuration: CompilerConfiguration,
        messageCollector: MessageCollector
    ): KotlinCoreEnvironment? {
        if (messageCollector.hasErrors()) return null

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        performanceManager.notifyCompilerInitialized()

        return if (messageCollector.hasErrors()) null else environment
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2JVMCompilerArguments, services: Services
    ) {
        with(configuration) {
            if (IncrementalCompilation.isEnabledForJvm()) {
                putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, services[LookupTracker::class.java])

                putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, services[ExpectActualTracker::class.java])

                putIfNotNull(
                    JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS,
                    services[IncrementalCompilationComponents::class.java]
                )

                putIfNotNull(JVMConfigurationKeys.JAVA_CLASSES_TRACKER, services[JavaClassesTracker::class.java])
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
}


fun main(args: Array<String>) = K2JVMCompiler.main(args)
