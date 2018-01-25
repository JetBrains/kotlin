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

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.jvm.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.cli.jvm.repl.configuration.ReplConfiguration
import org.jetbrains.kotlin.cli.jvm.repl.configuration.IdeReplConfiguration
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class KotlinRepl(
        disposable: Disposable,
        compilerConfiguration: CompilerConfiguration,
        private val replConfiguration: ReplConfiguration
) {
    private val replInitializer: Future<ReplInterpreter> = Executors.newSingleThreadExecutor().submit(Callable {
        ReplInterpreter(disposable, compilerConfiguration, replConfiguration)
    })

    private val replInterpreter: ReplInterpreter
        get() = replInitializer.get()

    val writer get() = replConfiguration.writer

    private val messageCollector = compilerConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    private fun doRun() {
        try {
            with(writer) {
                printlnWelcomeMessage(
                    "Welcome to Kotlin version ${KotlinCompilerVersion.VERSION} " +
                            "(JRE ${System.getProperty("java.runtime.version")})"
                )
                printlnWelcomeMessage("Type :help for help, :quit for quit")
            }

            // Display compiler messages related to configuration and CLI arguments, quit if there are errors
            val hasErrors = messageCollector.hasErrors()
            (messageCollector as? GroupingMessageCollector)?.flush()
            if (hasErrors) return

            val processor = ReplProcessor(replInterpreter, replConfiguration)
            replConfiguration.reader.runLoop(processor)
        } catch (e: Exception) {
            replConfiguration.exceptionReporter.report(e)
            throw e
        } finally {
            try {
                replConfiguration.reader.flushHistory()
            } catch (e: Exception) {
                replConfiguration.exceptionReporter.report(e)
                throw e
            }
        }
    }

    enum class WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT
    }

    companion object {
        fun run(disposable: Disposable, configuration: CompilerConfiguration) {
            val replIdeMode = System.getProperty("kotlin.repl.ideMode") == "true"
            val replConfiguration = if (replIdeMode) IdeReplConfiguration() else ConsoleReplConfiguration()

            return try {
                KotlinRepl(disposable, configuration, replConfiguration).doRun()
            } catch (e: Exception) {
                replConfiguration.exceptionReporter.report(e)
                throw e
            }
        }
    }

}