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

package org.jetbrains.kotlin.cli.jvm.repl.configuration

import org.jetbrains.kotlin.cli.jvm.repl.IdeReplExceptionReporter
import org.jetbrains.kotlin.cli.jvm.repl.ReplExceptionReporter
import org.jetbrains.kotlin.cli.jvm.repl.messages.IdeDiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.reader.IdeReplCommandReader
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplSystemInWrapper
import org.jetbrains.kotlin.cli.jvm.repl.writer.IdeSystemOutWrapperReplWriter
import org.jetbrains.kotlin.cli.jvm.repl.writer.ReplWriter

class IdeReplConfiguration : ReplConfiguration {
    override val allowIncompleteLines: Boolean
        get() = false

    override val executionInterceptor: SnippetExecutionInterceptor = object : SnippetExecutionInterceptor {
        override fun <T> execute(block: () -> T): T {
            try {
                sinWrapper.isReplScriptExecuting = true
                return block()
            } finally {
                sinWrapper.isReplScriptExecuting = false
            }
        }
    }

    override fun createDiagnosticHolder() = IdeDiagnosticMessageHolder()

    override val writer: ReplWriter
    override val exceptionReporter: ReplExceptionReporter
    override val commandReader: ReplCommandReader

    val sinWrapper: ReplSystemInWrapper

    init {
        // wrapper for `out` is required to escape every input in [ideMode];
        // if [ideMode == false] then just redirects all input to [System.out]
        // if user calls [System.setOut(...)] then undefined behaviour
        val soutWrapper = IdeSystemOutWrapperReplWriter(System.out)
        System.setOut(soutWrapper)

        // wrapper for `in` is required to give user possibility of calling
        // [readLine] from ide-console repl
        sinWrapper = ReplSystemInWrapper(System.`in`, soutWrapper)
        System.setIn(sinWrapper)

        writer = soutWrapper
        exceptionReporter = IdeReplExceptionReporter(writer)
        commandReader = IdeReplCommandReader()
    }
}
