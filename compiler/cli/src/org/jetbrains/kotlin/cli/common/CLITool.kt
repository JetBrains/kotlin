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
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageFeature.Kind.BUG_FIX
import org.jetbrains.kotlin.config.LanguageFeature.State.ENABLED
import org.jetbrains.kotlin.config.Services
import java.io.PrintStream
import java.net.URL
import java.net.URLConnection
import java.util.function.Predicate

abstract class CLITool<A : CommonToolArguments> {
    fun exec(errStream: PrintStream, vararg args: String): ExitCode {
        val rendererType = System.getProperty(MessageRenderer.PROPERTY_KEY)

        val messageRenderer = when (rendererType) {
            MessageRenderer.XML.name -> MessageRenderer.XML
            MessageRenderer.GRADLE_STYLE.name -> MessageRenderer.GRADLE_STYLE
            MessageRenderer.WITHOUT_PATHS.name -> MessageRenderer.WITHOUT_PATHS
            MessageRenderer.PLAIN_FULL_PATHS.name -> MessageRenderer.PLAIN_FULL_PATHS
            else -> MessageRenderer.PLAIN_RELATIVE_PATHS
        }

        return exec(errStream, Services.EMPTY, messageRenderer, args)
    }

    protected fun exec(
        errStream: PrintStream,
        services: Services,
        messageRenderer: MessageRenderer,
        args: Array<out String>
    ): ExitCode {
        val arguments = createArguments()
        parseCommandLineArguments(args.asList(), arguments)
        val collector = PrintingMessageCollector(errStream, messageRenderer, arguments.verbose)

        try {
            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemInstall()
            }

            errStream.print(messageRenderer.renderPreamble())

            val errorMessage = validateArguments(arguments.errors)
            if (errorMessage != null) {
                collector.report(CompilerMessageSeverity.ERROR, errorMessage, null)
                collector.report(CompilerMessageSeverity.INFO, "Use -help for more information", null)
                return ExitCode.COMPILATION_ERROR
            }

            if (arguments.help || arguments.extraHelp) {
                errStream.print(messageRenderer.renderUsage(Usage.render(this, arguments)))
                return ExitCode.OK
            }

            return exec(collector, services, arguments)
        } finally {
            errStream.print(messageRenderer.renderConclusion())

            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemUninstall()
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

    private fun reportArgumentParseProblems(collector: MessageCollector, arguments: A) {
        reportUnsafeInternalArgumentsIfAny(arguments, collector)

        val errors = arguments.errors ?: return

        for (flag in errors.unknownExtraFlags) {
            collector.report(STRONG_WARNING, "Flag is not supported by this version of the compiler: $flag")
        }
        for (argument in errors.extraArgumentsPassedInObsoleteForm) {
            collector.report(
                STRONG_WARNING, "Advanced option value is passed in an obsolete form. Please use the '=' character " +
                        "to specify the value: $argument=..."
            )
        }
        for ((key, value) in errors.duplicateArguments) {
            collector.report(STRONG_WARNING, "Argument $key is passed multiple times. Only the last value will be used: $value")
        }
        for ((deprecatedName, newName) in errors.deprecatedArguments) {
            collector.report(STRONG_WARNING, "Argument $deprecatedName is deprecated. Please use $newName instead")
        }

        for (argfileError in errors.argfileErrors) {
            collector.report(STRONG_WARNING, argfileError)
        }

        for (internalArgumentsError in errors.internalArgumentsParsingProblems) {
            collector.report(STRONG_WARNING, internalArgumentsError)
        }
    }

    private fun reportUnsafeInternalArgumentsIfAny(arguments: A, collector: MessageCollector) {
        val unsafeArguments = arguments.internalArguments.filterNot {
            // -XXLanguage which turns on BUG_FIX considered safe
            it is ManualLanguageFeatureSetting && it.languageFeature.kind == BUG_FIX && it.state == ENABLED
        }

        if (unsafeArguments.isNotEmpty()) {
            val unsafeArgumentsString = unsafeArguments.joinToString(prefix = "\n", postfix = "\n\n", separator = "\n") {
                it.stringRepresentation
            }

            collector.report(
                STRONG_WARNING,
                "ATTENTION!\n" +
                        "This build uses unsafe internal compiler arguments:\n" +
                        unsafeArgumentsString +
                        "This mode is not recommended for production use,\n" +
                        "as no stability/compatibility guarantees are given on\n" +
                        "compiler or generated code. Use it at your own risk!\n"
            )
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
            if (System.getProperty(PlainTextMessageRenderer.KOTLIN_COLORS_ENABLED_PROPERTY) == null) {
                System.setProperty(PlainTextMessageRenderer.KOTLIN_COLORS_ENABLED_PROPERTY, "true")
            }
            val exitCode = doMainNoExit(compiler, args)
            if (exitCode != ExitCode.OK) {
                System.exit(exitCode.code)
            }
        }

        @JvmStatic
        fun doMainNoExit(compiler: CLITool<*>, args: Array<String>): ExitCode = try {
            compiler.exec(System.err, *args)
        } catch (e: CompileEnvironmentException) {
            System.err.println(e.message)
            ExitCode.INTERNAL_ERROR
        }
    }
}
