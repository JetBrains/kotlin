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

package org.jetbrains.kotlin.cli.jvm.repl.reader

import org.jetbrains.kotlin.cli.common.repl.replUnescapeLineBreaks
import org.jetbrains.kotlin.cli.jvm.repl.writer.ReplWriter
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ReplSystemInWrapper(
    private val stdin: InputStream,
    private val replWriter: ReplWriter
) : InputStream() {
    private var isXmlIncomplete = true
    private var isLastByteProcessed = false
    private var isReadLineStartSent = false
    private var byteBuilder = ByteArrayOutputStream()
    private var curBytePos = 0
    private var inputByteArray = byteArrayOf()

    private val isAtBufferEnd: Boolean
        get() = curBytePos == inputByteArray.size

    @Volatile
    var isReplScriptExecuting = false

    override fun read(): Int {
        if (isLastByteProcessed) {
            if (isReplScriptExecuting) {
                isReadLineStartSent = false
                replWriter.notifyReadLineEnd()
            }

            isLastByteProcessed = false
            return -1
        }

        while (isXmlIncomplete) {
            if (!isReadLineStartSent && isReplScriptExecuting) {
                replWriter.notifyReadLineStart()
                isReadLineStartSent = true
            }

            byteBuilder.write(stdin.read())

            if (byteBuilder.toString().endsWith('\n')) {
                isXmlIncomplete = false
                isLastByteProcessed = false

                inputByteArray = parseInput().toByteArray()
            }
        }

        val nextByte = inputByteArray[curBytePos++].toInt()
        resetBufferIfNeeded()
        return nextByte
    }

    private fun parseInput(): String {
        val xmlInput = byteBuilder.toString()
        val unescapedXml = parseXml(xmlInput)

        return if (isReplScriptExecuting)
            unescapedXml.replUnescapeLineBreaks()
        else
            unescapedXml
    }

    private fun resetBufferIfNeeded() {
        if (isAtBufferEnd) {
            isXmlIncomplete = true
            byteBuilder = ByteArrayOutputStream()
            curBytePos = 0
            isLastByteProcessed = true
        }
    }
}

private fun parseXml(inputMessage: String): String {
    fun strToSource(s: String) = InputSource(ByteArrayInputStream(s.toByteArray()))

    val docFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = docFactory.newDocumentBuilder()
    val input = docBuilder.parse(strToSource(inputMessage))

    val root = input.firstChild as Element
    return root.textContent
}
