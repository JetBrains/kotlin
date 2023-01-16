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

package org.jetbrains.kotlin.cli.common

import org.fusesource.jansi.AnsiConsole
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArgumentsFromEnvironment
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.utils.PathUtil
import java.io.PrintStream
import java.net.URL
import java.net.URLConnection
import java.util.function.Predicate
import kotlin.system.exitProcess

abstract class CLITool<A : CommonToolArguments> {
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
                collector.report(CompilerMessageSeverity.ERROR, errorMessage, null)
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
            FilteringMessageCollector(messageCollector, Predicate.isEqual(CompilerMessageSeverity.WARNING))
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

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase)
    protected abstract fun execImpl(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode

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

    var isReadingSettingsFromEnvironmentAllowed: Boolean =
        this::class.java.classLoader.getResource(LanguageVersionSettings.RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT) != null

    companion object {
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
        fun doMain(compiler: CLITool<*>, args: Array<String>) {
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
            compiler: CLITool<*>,
            args: Array<String>,
            messageRenderer: MessageRenderer = defaultMessageRenderer()
        ): ExitCode = try {
            compiler.exec(System.err, messageRenderer, *args)
        } catch (e: CompileEnvironmentException) {
            System.err.println(e.message)
            ExitCode.INTERNAL_ERROR
        }
    }
}
