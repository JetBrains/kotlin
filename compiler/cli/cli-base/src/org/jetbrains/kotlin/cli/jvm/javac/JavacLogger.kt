/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.javac

import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.PrintWriter
import java.io.Writer

class JavacLogger(
    context: Context,
    errorWriter: PrintWriter,
    warningWriter: PrintWriter,
    infoWriter: PrintWriter
) : Log(context, errorWriter, warningWriter, infoWriter) {
    init {
        context.put(Log.outKey, infoWriter)
    }

    override fun printLines(kind: WriterKind, message: String, vararg args: Any?) {}

    companion object {
        fun preRegister(context: Context, messageCollector: MessageCollector) {
            context.put(Log.logKey, Context.Factory<Log> {
                JavacLogger(
                    it,
                    PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.ERROR)),
                    PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.WARNING)),
                    PrintWriter(MessageCollectorAdapter(messageCollector, CompilerMessageSeverity.INFO))
                )
            })
        }
    }
}

private class MessageCollectorAdapter(
    private val messageCollector: MessageCollector,
    private val severity: CompilerMessageSeverity
) : Writer() {
    override fun write(buffer: CharArray, offset: Int, length: Int) {
        if (length == 1 && buffer[0] == '\n') return

        messageCollector.report(severity, String(buffer, offset, length))
    }

    override fun flush() {
        (messageCollector as? GroupingMessageCollector)?.flush()
    }

    override fun close() = flush()
}
