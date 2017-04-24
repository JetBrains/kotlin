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
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import java.io.PrintStream
import java.util.function.Predicate

abstract class CLITool<A : CommonToolArguments> {
    fun exec(errStream: PrintStream, vararg args: String): ExitCode {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_RELATIVE_PATHS, args)
    }

    protected fun exec(
            errStream: PrintStream,
            services: Services,
            messageRenderer: MessageRenderer,
            args: Array<out String>
    ): ExitCode {
        K2JVMCompiler.resetInitStartTime()

        val parseArgumentsCollector = PrintingMessageCollector(errStream, messageRenderer, false)
        val arguments = try {
            parseArguments(parseArgumentsCollector, args) ?: return ExitCode.INTERNAL_ERROR
        }
        catch (e: IllegalArgumentException) {
            parseArgumentsCollector.report(CompilerMessageSeverity.ERROR, e.message!!, null)
            parseArgumentsCollector.report(CompilerMessageSeverity.INFO, "Use -help for more information", null)
            return ExitCode.COMPILATION_ERROR
        }

        if (arguments.help || arguments.extraHelp) {
            Usage.print(errStream, this, arguments)
            return ExitCode.OK
        }

        val collector = PrintingMessageCollector(errStream, messageRenderer, arguments.verbose)

        try {
            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemInstall()
            }

            errStream.print(messageRenderer.renderPreamble())
            return exec(collector, services, arguments)
        }
        finally {
            errStream.print(messageRenderer.renderConclusion())

            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemUninstall()
            }
        }
    }

    fun exec(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode {
        printVersionIfNeeded(messageCollector, arguments)

        val fixedMessageCollector = if (arguments.suppressWarnings) {
            FilteringMessageCollector(messageCollector, Predicate.isEqual(CompilerMessageSeverity.WARNING))
        }
        else {
            messageCollector
        }

        reportArgumentParseProblems(fixedMessageCollector, arguments.errors)
        return execImpl(fixedMessageCollector, services, arguments)
    }

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase)
    protected abstract fun execImpl(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode

    private fun parseArguments(messageCollector: MessageCollector, args: Array<out String>): A? {
        return try {
            createArguments().also { parseArguments(args, it) }
        }
        catch (e: IllegalArgumentException) {
            throw e
        }
        catch (t: Throwable) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(t), null)
            null
        }
    }

    protected abstract fun createArguments(): A

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase) and in kotlin-gradle-plugin (KotlinJvmOptionsImpl, KotlinJsOptionsImpl)
    fun parseArguments(args: Array<out String>, arguments: A) {
        parseCommandLineArguments(args, arguments)
        val message = validateArguments(arguments.errors)
        if (message != null) {
            throw IllegalArgumentException(message)
        }
    }

    private fun reportArgumentParseProblems(collector: MessageCollector, errors: ArgumentParseErrors) {
        for (flag in errors.unknownExtraFlags) {
            collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Flag is not supported by this version of the compiler: " + flag, null)
        }
        for (argument in errors.extraArgumentsPassedInObsoleteForm) {
            collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Advanced option value is passed in an obsolete form. Please use the '=' character " +
                    "to specify the value: " + argument + "=...", null)
        }
        for ((key, value) in errors.duplicateArguments) {
            collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Argument $key is passed multiple times. Only the last value will be used: $value", null)
        }
    }

    protected fun <A : CommonToolArguments> printVersionIfNeeded(messageCollector: MessageCollector, arguments: A) {
        if (!arguments.version) return

        if (arguments.version) {
            val jreVersion = System.getProperty("java.runtime.version")
            messageCollector.report(CompilerMessageSeverity.INFO,
                                    "${executableScriptFileName()} ${KotlinCompilerVersion.VERSION} (JRE $jreVersion)",
                                    null
            )
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
            System.setProperty("java.awt.headless", "true")
            val exitCode = doMainNoExit(compiler, args)
            if (exitCode != ExitCode.OK) {
                System.exit(exitCode.code)
            }
        }

        @JvmStatic
        fun doMainNoExit(compiler: CLITool<*>, args: Array<String>): ExitCode {
            try {
                return compiler.exec(System.err, *args)
            }
            catch (e: CompileEnvironmentException) {
                System.err.println(e.message)
                return ExitCode.INTERNAL_ERROR
            }
        }
    }
}