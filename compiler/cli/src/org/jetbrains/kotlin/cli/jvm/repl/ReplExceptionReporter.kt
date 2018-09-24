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

package org.jetbrains.kotlin.cli.jvm.repl

import org.jetbrains.kotlin.cli.jvm.repl.writer.ReplWriter
import java.io.PrintWriter
import java.io.StringWriter

interface ReplExceptionReporter {
    fun report(e: Throwable)

    companion object DoNothing : ReplExceptionReporter {
        override fun report(e: Throwable) {}
    }
}

class IdeReplExceptionReporter(private val replWriter: ReplWriter) : ReplExceptionReporter {
    override fun report(e: Throwable) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)

        val writerString = stringWriter.toString()
        val internalErrorText = if (writerString.isEmpty()) "Unknown error" else writerString

        replWriter.sendInternalErrorReport(internalErrorText)
    }
}
