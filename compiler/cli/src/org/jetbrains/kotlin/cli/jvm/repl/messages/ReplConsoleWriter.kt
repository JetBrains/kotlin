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

package org.jetbrains.kotlin.cli.jvm.repl.messages

class ReplConsoleWriter : ReplWriter {
    init {
        Runtime.getRuntime().addShutdownHook(object : Thread("Kotlin REPL shutdown hook") {
            override fun run() {
                // Print an empty line to make the command prompt start from the beginning of the line after REPL exits.
                // This is essentially a workaround of a jline2 bug.
                // TODO: remove this as soon as we update to jline3
                println()
            }
        })
    }

    override fun printlnWelcomeMessage(x: String) = println(x)
    override fun printlnHelpMessage(x: String) = println(x)
    override fun outputCompileError(x: String) = println(x)
    override fun outputCommandResult(x: String) = println(x)
    override fun outputRuntimeError(x: String) = println(x)

    override fun notifyReadLineStart() {}
    override fun notifyReadLineEnd() {}
    override fun notifyIncomplete() {}
    override fun notifyCommandSuccess() {}
    override fun sendInternalErrorReport(x: String) {}
}