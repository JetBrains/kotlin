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

package org.jetbrains.kotlin.javac

import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.PrintWriter
import java.io.Writer

class JavacLogger(context: Context,
               errorWriter: PrintWriter,
               warningWriter: PrintWriter,
               infoWriter: PrintWriter) : Log(context, errorWriter, warningWriter, infoWriter) {
    init {
        context.put(Log.outKey, infoWriter)
    }

    override fun printLines(kind: WriterKind, message: String, vararg args: Any?) {}

    companion object {
        fun preRegister(context: Context, messageCollector: MessageCollector) {
            context.put(Log.logKey, Context.Factory<Log> {
                JavacLogger(it,
                            PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.ERROR)),
                            PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.WARNING)),
                            PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.INFO)))
            })
        }
    }

}

private class MessageCollectorAdapter(private val messageCollector: MessageCollector,
                                      private val severity: CompilerMessageSeverity) : Writer() {

    override fun write(buffer: CharArray, offset: Int, length: Int) {
        if (length > 1) {
            messageCollector.report(severity, String(buffer, offset, length))
        }
    }

    override fun flush() {
        (messageCollector as? GroupingMessageCollector)?.flush()
    }

    override fun close() = flush()

}