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

package org.jetbrains.kotlin.cli.jvm.repl.writer

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.LineSeparator
import org.jetbrains.kotlin.cli.jvm.repl.messages.SOURCE_CHARS
import org.jetbrains.kotlin.cli.jvm.repl.messages.XML_REPLACEMENTS
import org.jetbrains.kotlin.utils.repl.ReplEscapeType
import java.io.PrintStream
import org.jetbrains.kotlin.utils.repl.ReplEscapeType.*

internal val END_LINE: String = LineSeparator.getSystemLineSeparator().separatorString
internal val XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

class IdeSystemOutWrapperReplWriter(standardOut: PrintStream) : PrintStream(standardOut, true), ReplWriter {
    override fun print(x: Boolean) = printWithEscaping(x.toString())
    override fun print(x: Char) = printWithEscaping(x.toString())
    override fun print(x: Int) = printWithEscaping(x.toString())
    override fun print(x: Long) = printWithEscaping(x.toString())
    override fun print(x: Float) = printWithEscaping(x.toString())
    override fun print(x: Double) = printWithEscaping(x.toString())
    override fun print(x: String) = printWithEscaping(x)
    override fun print(x: Any?) = printWithEscaping(x.toString())

    private fun printlnWithEscaping(text: String, escapeType: ReplEscapeType = USER_OUTPUT) {
        printWithEscaping("$text\n", escapeType)
    }

    private fun printWithEscaping(text: String, escapeType: ReplEscapeType = USER_OUTPUT) {
        super.print("${xmlEscape(text, escapeType)}${END_LINE}")
    }

    private fun xmlEscape(s: String, escapeType: ReplEscapeType): String {
        val singleLine = StringUtil.replace(s, SOURCE_CHARS, XML_REPLACEMENTS)
        return "${XML_PREAMBLE}<output type=\"$escapeType\">${StringUtil.escapeXml(singleLine)}</output>"
    }

    override fun printlnWelcomeMessage(x: String) = printlnWithEscaping(x, INITIAL_PROMPT)
    override fun printlnHelpMessage(x: String) = printlnWithEscaping(x, HELP_PROMPT)
    override fun outputCommandResult(x: String) = printlnWithEscaping(x, REPL_RESULT)
    override fun notifyReadLineStart() = printlnWithEscaping("", READLINE_START)
    override fun notifyReadLineEnd() = printlnWithEscaping("", READLINE_END)
    override fun notifyCommandSuccess() = printlnWithEscaping("", SUCCESS)
    override fun notifyIncomplete() = printlnWithEscaping("", REPL_INCOMPLETE)
    override fun outputCompileError(x: String) = printlnWithEscaping(x, COMPILE_ERROR)
    override fun outputRuntimeError(x: String) = printlnWithEscaping(x, RUNTIME_ERROR)
    override fun sendInternalErrorReport(x: String) = printlnWithEscaping(x, INTERNAL_ERROR)
}
