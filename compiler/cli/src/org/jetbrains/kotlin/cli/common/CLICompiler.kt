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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLConnection
import java.util.function.Predicate
import kotlin.system.exitProcess

abstract class CLICompiler<A : CommonCompilerArguments> {

    abstract val defaultPerformanceManager: CommonCompilerPerformanceManager

    var isReadingSettingsFromEnvironmentAllowed: Boolean =
        this::class.java.classLoader.getResource(LanguageVersionSettings.RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT) != null

    protected open fun createPerformanceManager(arguments: A, services: Services): CommonCompilerPerformanceManager =
        defaultPerformanceManager

    // Used in CompilerRunnerUtil#invokeExecMethod, in Eclipse plugin (KotlinCLICompiler) and in kotlin-gradle-plugin (GradleCompilerRunner)
    fun execAndOutputXml(errStream: PrintStream, services: Services, vararg args: String): ExitCode {
        return exec(errStream, services, MessageRenderer.XML, args)
    }

    // Used via reflection in KotlinCompilerBaseTask
    fun execFullPathsInMessages(errStream: PrintStream, args: Array<String>): ExitCode {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_FULL_PATHS, args)
    }

    private fun execImpl(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode {
        val performanceManager = createPerformanceManager(arguments, services)
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableCollectingPerformanceStatistics()
        }

        val configuration = CompilerConfiguration()

        configuration.put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)

        val collector = GroupingMessageCollector(messageCollector, arguments.allWarningsAsErrors, arguments.reportAllWarnings).also {
            configuration.messageCollector = it
        }

        configuration.put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
        try {
            setupCommonArguments(configuration, arguments)
            setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
            val paths = computeKotlinPaths(collector, arguments)
            if (collector.hasErrors()) {
                return COMPILATION_ERROR
            }

            val canceledStatus = services[CompilationCanceledStatus::class.java]
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)

            val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
            try {
                setIdeaIoUseFallback()

                val code = doExecute(arguments, configuration, rootDisposable, paths)

                performanceManager.notifyCompilationFinished()
                if (arguments.reportPerf) {
                    collector.report(LOGGING, "PERF: " + performanceManager.getTargetInfo())
                    for (measurement in performanceManager.getMeasurementResults()) {
                        collector.report(LOGGING, "PERF: " + measurement.render(), null)
                    }
                }

                if (arguments.dumpPerf != null) {
                    performanceManager.dumpPerformanceReport(File(arguments.dumpPerf!!))
                }

                return if (collector.hasErrors()) COMPILATION_ERROR else code
            } catch (e: CompilationCanceledException) {
                collector.reportCompilationCancelled(e)
                return OK
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is CompilationCanceledException) {
                    collector.reportCompilationCancelled(cause)
                    return OK
                } else {
                    throw e
                }
            } finally {
                Disposer.dispose(rootDisposable)
            }
        } catch (e: CompilationErrorException) {
            return COMPILATION_ERROR
        } catch (t: Throwable) {
            MessageCollectorUtil.reportException(collector, t)
            return if (t is OutOfMemoryError || t.hasOOMCause()) OOM_ERROR else INTERNAL_ERROR
        } finally {
            collector.flush()
        }
    }

    private fun Throwable.hasOOMCause(): Boolean = when (cause) {
        is OutOfMemoryError -> true
        else -> cause?.hasOOMCause() ?: false
    }

    private fun MessageCollector.reportCompilationCancelled(e: CompilationCanceledException) {
        if (e !is IncrementalNextRoundException) {
            report(INFO, "Compilation was canceled", null)
        }
    }

    private fun setupCommonArguments(configuration: CompilerConfiguration, arguments: A) {
        configuration.setupCommonArguments(arguments, this::createMetadataVersion)
    }

    protected abstract fun createMetadataVersion(versionArray: IntArray): BinaryVersion

    protected abstract fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: A, services: Services
    )

    protected abstract fun doExecute(
        arguments: A,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode

    protected abstract fun MutableList<String>.addPlatformOptions(arguments: A)

    protected fun loadPlugins(paths: KotlinPaths?, arguments: A, configuration: CompilerConfiguration): ExitCode {
        val pluginClasspaths = arguments.pluginClasspaths.orEmpty().toMutableList()
        val pluginOptions = arguments.pluginOptions.orEmpty().toMutableList()
        val pluginConfigurations = arguments.pluginConfigurations.orEmpty().toMutableList()
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val useK2 = configuration.get(CommonConfigurationKeys.USE_FIR) == true

        val scriptingPluginClasspath = mutableListOf<String>()
        val scriptingPluginOptions = mutableListOf<String>()

        if (!arguments.disableDefaultScriptingPlugin) {
            scriptingPluginOptions.addPlatformOptions(arguments)
            val explicitScriptingPlugin =
                extractPluginClasspathAndOptions(pluginConfigurations).any { (_, classpath, _) ->
                    classpath.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) }
                } || pluginClasspaths.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) }
            val explicitOrLoadedScriptingPlugin = explicitScriptingPlugin ||
                    tryLoadScriptingPluginFromCurrentClassLoader(configuration, pluginOptions, useK2)
            if (!explicitOrLoadedScriptingPlugin) {
                val kotlinPaths = paths ?: PathUtil.kotlinPathsForCompiler
                val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
                val (jars, missingJars) =
                    PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }.partition { it.exists() }
                if (missingJars.isEmpty()) {
                    scriptingPluginClasspath.addAll(0, jars.map { it.canonicalPath })
                } else {
                    messageCollector.report(
                        LOGGING,
                        "Scripting plugin will not be loaded: not all required jars are present in the classpath (missing files: $missingJars)"
                    )
                }
            }
        } else {
            scriptingPluginOptions.add("plugin:kotlin.scripting:disable=true")
        }

        pluginClasspaths.addAll(scriptingPluginClasspath)
        pluginOptions.addAll(scriptingPluginOptions)

        if (!checkPluginsArguments(messageCollector, useK2, pluginClasspaths, pluginOptions, pluginConfigurations)) {
            return INTERNAL_ERROR
        }

        return PluginCliParser.loadPluginsSafe(pluginClasspaths, pluginOptions, pluginConfigurations, configuration)
    }

    private fun tryLoadScriptingPluginFromCurrentClassLoader(
        configuration: CompilerConfiguration,
        pluginOptions: List<String>,
        useK2: Boolean
    ): Boolean =
        try {
            val pluginRegistrarClass = PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_REGISTRAR_NAME)
            val pluginRegistrar = (pluginRegistrarClass.getDeclaredConstructor().newInstance() as? ComponentRegistrar)?.also {
                configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, it)
            }
            val pluginK2Registrar = if (useK2) {
                val pluginK2RegistrarClass = PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_K2_REGISTRAR_NAME)
                (pluginK2RegistrarClass.getDeclaredConstructor().newInstance() as? CompilerPluginRegistrar)?.also {
                    configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it)
                }
            } else null
            if (pluginRegistrar != null || pluginK2Registrar != null) {
                processScriptPluginCliOptions(pluginOptions, configuration)
                true
            } else false
        } catch (e: Throwable) {
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            messageCollector.report(LOGGING, "Exception on loading scripting plugin: $e")
            false
        }

    private fun processScriptPluginCliOptions(pluginOptions: List<String>, configuration: CompilerConfiguration) {
        val cmdlineProcessorClass =
            if (pluginOptions.isEmpty()) null
            else PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME)!!
        val cmdlineProcessor = cmdlineProcessorClass?.getDeclaredConstructor()?.newInstance() as? CommandLineProcessor
        if (cmdlineProcessor != null) {
            processCompilerPluginsOptions(configuration, pluginOptions, listOf(cmdlineProcessor))
        }
    }

    fun exec(errStream: PrintStream, vararg args: String): ExitCode =
        exec(errStream, Services.EMPTY, defaultMessageRenderer(), args)

    fun exec(errStream: PrintStream, messageRenderer: MessageRenderer, vararg args: String): ExitCode =
        exec(errStream, Services.EMPTY, messageRenderer, args)

    protected fun exec(
        errStream: PrintStream,
        services: Services,
        messageRenderer: MessageRenderer,
        args: Array<out String>
    ): ExitCode {
        val arguments = createArguments()
        parseCommandLineArguments(args.asList(), arguments)

        if (isReadingSettingsFromEnvironmentAllowed) {
            parseCommandLineArgumentsFromEnvironment(arguments)
        }

        val collector = PrintingMessageCollector(errStream, messageRenderer, arguments.verbose)

        try {
            if (messageRenderer is PlainTextMessageRenderer) {
                messageRenderer.enableColorsIfNeeded()
            }

            errStream.print(messageRenderer.renderPreamble())

            val errorMessage = validateArguments(arguments.errors)
            if (errorMessage != null) {
                collector.report(ERROR, errorMessage, null)
                collector.report(INFO, "Use -help for more information", null)
                return COMPILATION_ERROR
            }

            if (arguments.help || arguments.extraHelp) {
                errStream.print(messageRenderer.renderUsage(Usage.render(this, arguments)))
                return OK
            }

            return exec(collector, services, arguments)
        } finally {
            errStream.print(messageRenderer.renderConclusion())

            if (messageRenderer is PlainTextMessageRenderer) {
                messageRenderer.disableColorsIfNeeded()
            }
        }
    }

    fun exec(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode {
        disableURLConnectionCaches()

        printVersionIfNeeded(messageCollector, arguments)

        val fixedMessageCollector = if (arguments.suppressWarnings && !arguments.allWarningsAsErrors) {
            FilteringMessageCollector(messageCollector, Predicate.isEqual(WARNING))
        } else {
            messageCollector
        }

        fixedMessageCollector.reportArgumentParseProblems(arguments)
        return execImpl(fixedMessageCollector, services, arguments)
    }

    private fun disableURLConnectionCaches() {
        // We disable caches to avoid problems with compiler under daemon, see https://youtrack.jetbrains.com/issue/KT-22513
        // For some inexplicable reason, URLConnection.setDefaultUseCaches is an instance method modifying a static field,
        // so we have to create a dummy instance to call that method

        object : URLConnection(URL("file:.")) {
            override fun connect() = throw UnsupportedOperationException()
        }.defaultUseCaches = false
    }

    abstract fun createArguments(): A

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase) and in kotlin-gradle-plugin (KotlinJvmOptionsImpl, KotlinJsOptionsImpl)
    fun parseArguments(args: Array<out String>, arguments: A) {
        parseCommandLineArguments(args.asList(), arguments)
        val message = validateArguments(arguments.errors)
        if (message != null) {
            throw IllegalArgumentException(message)
        }
    }

    private fun <A : CommonToolArguments> printVersionIfNeeded(messageCollector: MessageCollector, arguments: A) {
        if (arguments.version) {
            val jreVersion = System.getProperty("java.runtime.version")
            messageCollector.report(INFO, "${executableScriptFileName()} ${KotlinCompilerVersion.VERSION} (JRE $jreVersion)")
        }
    }

    abstract fun executableScriptFileName(): String

    companion object {
        const val SCRIPT_PLUGIN_REGISTRAR_NAME =
            "org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar"
        const val SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME = "org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCommandLineProcessor"
        const val SCRIPT_PLUGIN_K2_REGISTRAR_NAME =
            "org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar"

        private fun defaultMessageRenderer(): MessageRenderer =
            when (System.getProperty(MessageRenderer.PROPERTY_KEY)) {
                MessageRenderer.XML.name -> MessageRenderer.XML
                MessageRenderer.GRADLE_STYLE.name -> MessageRenderer.GRADLE_STYLE
                MessageRenderer.XCODE_STYLE.name -> MessageRenderer.XCODE_STYLE
                MessageRenderer.WITHOUT_PATHS.name -> MessageRenderer.WITHOUT_PATHS
                MessageRenderer.PLAIN_FULL_PATHS.name -> MessageRenderer.PLAIN_FULL_PATHS
                else -> MessageRenderer.PLAIN_RELATIVE_PATHS
            }

        /**
         * Useful main for derived command line tools
         */
        @JvmStatic
        fun doMain(compiler: CLICompiler<*>, args: Array<String>) {
            // We depend on swing (indirectly through PSI or something), so we want to declare headless mode,
            // to avoid accidentally starting the UI thread
            if (System.getProperty("java.awt.headless") == null) {
                System.setProperty("java.awt.headless", "true")
            }
            if (CompilerSystemProperties.KOTLIN_COLORS_ENABLED_PROPERTY.value == null) {
                CompilerSystemProperties.KOTLIN_COLORS_ENABLED_PROPERTY.value = "true"
            }

            setupIdeaStandaloneExecution()

            val exitCode = doMainNoExit(compiler, args)
            if (exitCode != ExitCode.OK) {
                exitProcess(exitCode.code)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun doMainNoExit(
            compiler: CLICompiler<*>,
            args: Array<String>,
            messageRenderer: MessageRenderer = defaultMessageRenderer()
        ): ExitCode = try {
            compiler.exec(System.err, messageRenderer, *args)
        } catch (e: CompileEnvironmentException) {
            System.err.println(e.message)
            INTERNAL_ERROR
        }
    }
}

fun checkPluginsArguments(
    messageCollector: MessageCollector,
    useK2: Boolean,
    pluginClasspaths: List<String>,
    pluginOptions: List<String>,
    pluginConfigurations: List<String>
): Boolean {
    var hasErrors = false

    for (classpath in pluginClasspaths) {
        if (!File(classpath).exists()) {
            messageCollector.report(ERROR, "Plugin classpath entry points to a non-existent location: $classpath")
        }
    }

    if (pluginConfigurations.isNotEmpty()) {
        messageCollector.report(WARNING, "Argument -Xcompiler-plugin is experimental")
        if (!useK2) {
            hasErrors = true
            messageCollector.report(
                ERROR,
                "-Xcompiler-plugin argument is allowed only for language version 2.0. Please use -Xplugin argument for language version 1.9 and below"
            )
        }
        if (pluginClasspaths.isNotEmpty() || pluginOptions.isNotEmpty()) {
            hasErrors = true
            val message = buildString {
                appendLine("Mixing legacy and modern plugin arguments is prohibited. Please use only one syntax")
                appendLine("Legacy arguments:")
                if (pluginClasspaths.isNotEmpty()) {
                    appendLine("  -Xplugin=${pluginClasspaths.joinToString(",")}")
                }
                pluginOptions.forEach {
                    appendLine("  -P $it")
                }
                appendLine("Modern arguments:")
                pluginConfigurations.forEach {
                    appendLine("  -Xcompiler-plugin=$it")
                }
            }
            messageCollector.report(ERROR, message)
        }
    }
    return !hasErrors
}

