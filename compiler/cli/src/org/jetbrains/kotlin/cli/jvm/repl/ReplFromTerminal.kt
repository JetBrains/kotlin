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
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.KotlinVersion
import org.jetbrains.kotlin.cli.jvm.repl.LineResult.*
import org.jetbrains.kotlin.cli.jvm.repl.messages.unescapeLineBreaks
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ReplFromTerminal(
        disposable: Disposable,
        compilerConfiguration: CompilerConfiguration,
        replConfiguration: ReplConfiguration
) : ReplConfiguration by replConfiguration {

    private val replInitializer: Future<ReplInterpreter> = Executors.newSingleThreadExecutor().submit(Callable {
        ReplInterpreter(disposable, compilerConfiguration, replConfiguration)
    })

    private val replInterpreter: ReplInterpreter
        get() = replInitializer.get()

    private fun doRun() {
        try {
            writer.printlnWelcomeMessage("Welcome to Kotlin version ${KotlinVersion.VERSION} " +
                                         "(JRE ${System.getProperty("java.runtime.version")})")
            writer.printlnWelcomeMessage("Type :help for help, :quit for quit")
            var next = WhatNextAfterOneLine.READ_LINE
            while (true) {
                next = one(next)
                if (next == WhatNextAfterOneLine.QUIT) {
                    break
                }
            }
        }
        catch (e: Exception) {
            errorLogger.logException(e)
        }
        finally {
            try {
                commandReader.flushHistory()
            }
            catch (e: Exception) {
                errorLogger.logException(e)
            }

        }
    }

    enum class WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT
    }

    private fun one(next: WhatNextAfterOneLine): WhatNextAfterOneLine {
        var line = commandReader.readLine(next) ?: return WhatNextAfterOneLine.QUIT

        line = unescapeLineBreaks(line)

        if (line.startsWith(":") && (line.length == 1 || line.get(1) != ':')) {
            val notQuit = oneCommand(line.substring(1))
            return if (notQuit) WhatNextAfterOneLine.READ_LINE else WhatNextAfterOneLine.QUIT
        }

        val lineResult = eval(line)
        if (lineResult is LineResult.Incomplete) {
            return WhatNextAfterOneLine.INCOMPLETE
        }
        else {
            return WhatNextAfterOneLine.READ_LINE
        }
    }

    private fun eval(line: String): LineResult {
        val lineResult = replInterpreter.eval(line)
        when (lineResult) {
            is ValueResult, UnitResult -> {
                writer.notifyCommandSuccess()
                if (lineResult is ValueResult) {
                    writer.outputCommandResult(lineResult.valueAsString)
                }
            }
            Incomplete -> writer.notifyIncomplete()
            is Error.CompileTime -> writer.outputCompileError(lineResult.errorText)
            is Error.Runtime -> writer.outputRuntimeError(lineResult.errorText)
        }
        return lineResult
    }

    @Throws(Exception::class)
    private fun oneCommand(command: String): Boolean {
        val split = splitCommand(command)
        if (split.size >= 1 && command == "help") {
            writer.printlnHelpMessage("Available commands:\n" +
                                      ":help                   show this help\n" +
                                      ":quit                   exit the interpreter\n" +
                                      ":dump bytecode          dump classes to terminal\n" +
                                      ":load <file>            load script from specified file")
            return true
        }
        else if (split.size >= 2 && split[0] == "dump" && split[1] == "bytecode") {
            replInterpreter.dumpClasses(PrintWriter(System.out))
            return true
        }
        else if (split.size >= 1 && split[0] == "quit") {
            return false
        }
        else if (split.size >= 2 && split[0] == "load") {
            val fileName = split[1]
            val scriptText = FileUtil.loadFile(File(fileName))
            eval(scriptText)
            return true
        }
        else {
            writer.printlnHelpMessage("Unknown command\n" + "Type :help for help")
            return true
        }
    }

    companion object {
        private fun splitCommand(command: String): List<String> {
            return Arrays.asList(*command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        }

        fun run(disposable: Disposable, configuration: CompilerConfiguration) {
            val replIdeMode = System.getProperty("kotlin.repl.ideMode") == "true"
            val replConfiguration = if (replIdeMode) ReplForIdeConfiguration() else ConsoleReplConfiguration()
            return try {
                ReplFromTerminal(disposable, configuration, replConfiguration).doRun()
            }
            catch (e: Exception) {
                replConfiguration.errorLogger.logException(e)
            }
        }
    }

}