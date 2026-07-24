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

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLConnection
import java.util.function.Predicate
import kotlin.system.exitProcess

abstract class CLICompiler<A : CommonCompilerArguments> {
    abstract val platform: TargetPlatform

    val defaultPerformanceManager: PerformanceManager by lazy {
        createPerformanceManagerFor(platform)
    }

    var isReadingSettingsFromEnvironmentAllowed: Boolean =
        this::class.java.classLoader.getResource(LanguageVersionSettings.RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT) != null

    protected open fun createPerformanceManager(arguments: A, services: Services): PerformanceManager =
        defaultPerformanceManager.apply {
            detailedPerf = arguments.detailedPerf
        }

    // Used in CompilerRunnerUtil#invokeExecMethod, in Eclipse plugin (KotlinCLICompiler) and in kotlin-gradle-plugin (GradleCompilerRunner)
    @Suppress("unused")
    fun execAndOutputXml(errStream: PrintStream, services: Services, vararg args: String): ExitCode {
        return exec(errStream, services, MessageRenderer.XML, args)
    }

    // Used via reflection in KotlinCompilerBaseTask
    fun execFullPathsInMessages(errStream: PrintStream, args: Array<String>): ExitCode {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_FULL_PATHS, args)
    }

    protected abstract fun createMetadataVersion(versionArray: IntArray): BinaryVersion

    /**
     * Main method for execution the new phased CLI compiler pipeline
     */
    protected abstract fun doExecutePhased(
        arguments: A,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode

    fun exec(errStream: PrintStream, vararg args: String): ExitCode =
        exec(errStream, Services.EMPTY, defaultMessageRenderer(), args)

    fun exec(errStream: PrintStream, messageRenderer: MessageRenderer, vararg args: String): ExitCode =
        exec(errStream, Services.EMPTY, messageRenderer, args)

    protected fun exec(
        errStream: PrintStream,
        services: Services,
        messageRenderer: MessageRenderer,
        args: Array<out String>,
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

            val errorMessages = validateArgumentsAllErrors(arguments.errors)
            if (errorMessages.isNotEmpty()) {
                errorMessages.forEach {
                    collector.report(ERROR, it, null)
                }
                collector.report(INFO, "Use -help for more information", null)
                return ExitCode.COMPILATION_ERROR
            }

            if (arguments.help || arguments.extraHelp) {
                errStream.print(messageRenderer.renderUsage(Usage.render(this, arguments)))
                return ExitCode.OK
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
        return doExecutePhased(arguments, services, fixedMessageCollector)
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
            messageRenderer: MessageRenderer = defaultMessageRenderer(),
        ): ExitCode = try {
            compiler.exec(System.err, messageRenderer, *args)
        } catch (e: CompileEnvironmentException) {
            System.err.println(e.message)
            ExitCode.INTERNAL_ERROR
        }
    }
}

fun checkPluginsArguments(
    configuration: CompilerConfiguration,
    useK2: Boolean,
    pluginClasspaths: List<String>,
    pluginOptions: List<String>,
    pluginConfigurations: List<String>,
): Boolean {
    var hasErrors = false

    for (classpath in pluginClasspaths) {
        if (!File(classpath).exists()) {
            configuration.report(COMPILER_ARGUMENTS_ERROR, "Plugin classpath entry points to a non-existent location: $classpath")
        }
    }

    if (pluginConfigurations.isNotEmpty()) {
        configuration.report(CliDiagnostics.COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL, "Argument -Xcompiler-plugin is experimental")

        if (!useK2) {
            hasErrors = true
            configuration.report(
                COMPILER_ARGUMENTS_ERROR,
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
            configuration.report(COMPILER_ARGUMENTS_ERROR, message)
        }
    }
    return !hasErrors
}

fun Throwable.hasOOMCause(): Boolean = when (cause) {
    is OutOfMemoryError -> true
    else -> cause?.hasOOMCause() ?: false
}

fun MessageCollector.reportCompilationCancelled(e: CompilationCanceledException) {
    if (e !is IncrementalNextRoundException) {
        report(INFO, "Compilation was canceled", null)
    }
}
