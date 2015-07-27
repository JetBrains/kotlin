/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil
import java.io.PrintStream

enum class EscapeType {
    ORDINARY, REPORT
}

public class ReplSystemOutWrapper(private val standardOut: PrintStream) : PrintStream(standardOut, true) {
    private val XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"

    private fun xmlEscape(s: String, escapeType: EscapeType): String {
        val singleLine = StringUtil.escapeLineBreak(s)
        return "$XML_PREFIX<output type=\"$escapeType\">${StringUtil.escapeXml(singleLine)}</output>"
    }

    override fun print(x: Any) = printWithEscaping(x.toString())
    private fun printWithEscaping(text: String, escapeType: EscapeType = EscapeType.ORDINARY) = super.print(xmlEscape(text, escapeType))

    override fun println(x: String) = printlnWithEscaping(x)
    override fun println(x: Any) = printlnWithEscaping(x.toString())
    public fun printlnWithEscaping(text: String, escapeType: EscapeType = EscapeType.ORDINARY): Unit = super.println(xmlEscape(text, escapeType))
}