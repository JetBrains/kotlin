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
import org.jetbrains.kotlin.cli.jvm.repl.messages.*
import org.jetbrains.kotlin.cli.jvm.repl.reader.ConsoleReplCommandReader
import org.jetbrains.kotlin.cli.jvm.repl.reader.IdeReplCommandReader
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.*

import java.io.File
import java.io.PrintWriter
import java.util.Arrays

class ReplFromTerminal(
        disposable: Disposable,
        compilerConfiguration: CompilerConfiguration
) {

    private var replInterpreter: ReplInterpreter? = null
    private var replInitializationFailed: Throwable? = null
    private val waitRepl = Object()

    private val ideMode: Boolean
    private var replReader: ReplSystemInWrapper? = null
    private val replWriter: ReplWriter
    private val replErrorLogger: ReplErrorLogger

    private var commandReader: ReplCommandReader? = null

    init {
        val replIdeMode = System.getProperty("kotlin.repl.ideMode")
        ideMode = replIdeMode != null && replIdeMode == "true"

        // wrapper for `out` is required to escape every input in [ideMode];
        // if [ideMode == false] then just redirects all input to [System.out]
        // if user calls [System.setOut(...)] then undefined behaviour
        if (ideMode) {
            val soutWrapper = ReplSystemOutWrapperForIde(System.out)
            replWriter = soutWrapper
            System.setOut(soutWrapper)
        }
        else {
            replWriter = ReplConsoleWriter()
        }

        // wrapper for `in` is required to give user possibility of calling
        // [readLine] from ide-console repl
        if (ideMode) {
            replReader = ReplSystemInWrapper(System.`in`, replWriter)
            System.setIn(replReader!!)
        }

        replErrorLogger = ReplErrorLogger(ideMode, replWriter)

        object : Thread("initialize-repl") {
            override fun run() {
                try {
                    replInterpreter = ReplInterpreter(disposable, compilerConfiguration, ideMode, replReader)
                }
                catch (e: Throwable) {
                    replInitializationFailed = e
                }

                synchronized (waitRepl) {
                    waitRepl.notifyAll()
                }
            }
        }.start()

        try {
            commandReader = createCommandReader()
        }
        catch (e: Exception) {
            replErrorLogger.logException(e)
        }

    }

    private fun createCommandReader(): ReplCommandReader {
        return if (ideMode)
            IdeReplCommandReader()
        else
            ConsoleReplCommandReader()
    }

    private fun getReplInterpreter(): ReplInterpreter {
        if (replInterpreter != null) {
            return replInterpreter!!
        }
        synchronized (waitRepl) {
            while (replInterpreter == null && replInitializationFailed == null) {
                try {
                    waitRepl.wait()
                }
                catch (e: Throwable) {
                    throw rethrow(e)
                }

            }
            if (replInterpreter != null) {
                return replInterpreter!!
            }
            throw rethrow(replInitializationFailed!!)
        }
    }

    private fun doRun() {
        try {
            replWriter.printlnWelcomeMessage("Welcome to Kotlin version ${KotlinVersion.VERSION} " +
                                             "(JRE ${System.getProperty("java.runtime.version")})")
            replWriter.printlnWelcomeMessage("Type :help for help, :quit for quit")
            var next = WhatNextAfterOneLine.READ_LINE
            while (true) {
                next = one(next)
                if (next == WhatNextAfterOneLine.QUIT) {
                    break
                }
            }
        }
        catch (e: Exception) {
            replErrorLogger.logException(e)
        }
        finally {
            try {
                commandReader!!.flushHistory()
            }
            catch (e: Exception) {
                replErrorLogger.logException(e)
            }

        }
    }

    enum class WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT
    }

    private fun one(next: WhatNextAfterOneLine): WhatNextAfterOneLine {
        try {
            var line = commandReader!!.readLine(next) ?: return WhatNextAfterOneLine.QUIT

            line = unescapeLineBreaks(line)

            if (line.startsWith(":") && (line.length == 1 || line.get(1) != ':')) {
                val notQuit = oneCommand(line.substring(1))
                return if (notQuit) WhatNextAfterOneLine.READ_LINE else WhatNextAfterOneLine.QUIT
            }

            val lineResultType = eval(line)
            if (lineResultType === ReplInterpreter.LineResultType.INCOMPLETE) {
                return WhatNextAfterOneLine.INCOMPLETE
            }
            else {
                return WhatNextAfterOneLine.READ_LINE
            }
        }
        catch (e: Exception) {
            throw rethrow(e)
        }

    }

    private fun eval(line: String): ReplInterpreter.LineResultType {
        val lineResult = getReplInterpreter().eval(line)
        if (lineResult.type === ReplInterpreter.LineResultType.SUCCESS) {
            replWriter.notifyCommandSuccess()
            if (!lineResult.isUnit) {
                replWriter.outputCommandResult(lineResult.value)
            }
        }
        else if (lineResult.type === ReplInterpreter.LineResultType.INCOMPLETE) {
            replWriter.notifyIncomplete()
        }
        else if (lineResult.type === ReplInterpreter.LineResultType.COMPILE_ERROR) {
            replWriter.outputCompileError(lineResult.errorText!!)
        }
        else if (lineResult.type === ReplInterpreter.LineResultType.RUNTIME_ERROR) {
            replWriter.outputRuntimeError(lineResult.errorText!!)
        }
        else {
            throw IllegalStateException("unknown line result type: " + lineResult)
        }
        return lineResult.type
    }

    @Throws(Exception::class)
    private fun oneCommand(command: String): Boolean {
        val split = splitCommand(command)
        if (split.size >= 1 && command == "help") {
            replWriter.printlnHelpMessage("Available commands:\n" +
                                          ":help                   show this help\n" +
                                          ":quit                   exit the interpreter\n" +
                                          ":dump bytecode          dump classes to terminal\n" +
                                          ":load <file>            load script from specified file")
            return true
        }
        else if (split.size >= 2 && split[0] == "dump" && split[1] == "bytecode") {
            getReplInterpreter().dumpClasses(PrintWriter(System.out))
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
            replWriter.printlnHelpMessage("Unknown command\n" + "Type :help for help")
            return true
        }
    }

    companion object {

        private fun splitCommand(command: String): List<String> {
            return Arrays.asList(*command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        }

        fun run(disposable: Disposable, configuration: CompilerConfiguration) {
            ReplFromTerminal(disposable, configuration).doRun()
        }
    }

}
