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

package org.jetbrains.kotlin.console

import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.xml.parsers.DocumentBuilderFactory

public class KotlinReplOutputHandler(
        process: Process,
        commandLine: String
) : OSProcessHandler(process, commandLine) {
    private val XML_START = "<?xml"
    private val dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val rangeQueue: Queue<TextRange> = ConcurrentLinkedQueue()

    override fun notifyTextAvailable(text: String, key: Key<*>?) {
        // skip "/usr/lib/jvm/java-8-oracle/bin/java -cp ..." intro
        if (!text.startsWith(XML_START)) return super.notifyTextAvailable(text, key)

        val output = dBuilder.parse(strToSource(text))
        val root = output.firstChild as Element
        val outputType = root.getAttribute("type")
        val content = StringUtil.unescapeStringCharacters(root.textContent).trim()

        when (outputType) {
            "ORDINARY" -> super.notifyTextAvailable("$content\n", key)
            "REPORT"   -> {
                val report = dBuilder.parse(strToSource(content, Charsets.UTF_16BE))
                val entries = report.getElementsByTagName("reportEntry")
                for (i in 0..entries.length - 1) {
                    val reportEntry = entries.item(i) as Element
                    val rangeStart = reportEntry.getAttribute("rangeStart").toInt()
                    val rangeEnd = reportEntry.getAttribute("rangeEnd").toInt()

                    rangeQueue.add(TextRange(rangeStart, rangeEnd))
                    super.notifyTextAvailable("${StringUtil.unescapeXml(reportEntry.textContent)}\n", key)
                }
            }
        }
    }

    private fun strToSource(s: String, encoding: Charset = Charsets.UTF_8) = InputSource(ByteArrayInputStream(s.toByteArray(encoding)))
}