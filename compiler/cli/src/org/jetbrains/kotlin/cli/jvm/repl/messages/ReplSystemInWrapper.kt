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

package org.jetbrains.kotlin.cli.jvm.repl.messages

import java.io.ByteArrayOutputStream
import java.io.InputStream

public class ReplSystemInWrapper(
        private val stdin: InputStream,
        private val replWriter: ReplSystemOutWrapper
) : InputStream() {
    private var isXmlIncomplete = true
    private var isLastScriptByteProcessed = false
    private var isReadLineStartSent = false
    private var byteBuilder = ByteArrayOutputStream()
    private var curBytePos = 0
    private var inputByteArray = byteArrayOf()

    private val isAtBufferEnd: Boolean
        get() = curBytePos == inputByteArray.size()

    var isReplScriptExecuting = false

    override fun read(): Int {
        if (isLastScriptByteProcessed && isReplScriptExecuting) {
            isLastScriptByteProcessed = false
            isReadLineStartSent = false
            replWriter.printlnReadLineEnd()
            return -1
        }

        while (isXmlIncomplete) {
            if (isReplScriptExecuting && !isReadLineStartSent) {
                replWriter.printlnReadLineStart()
                isReadLineStartSent = true
            }

            byteBuilder.write(stdin.read())

            if (byteBuilder.toString().endsWith(END_LINE)) {
                isXmlIncomplete = false
                isLastScriptByteProcessed = false

                inputByteArray = unescapedInput().toByteArray()
            }
        }

        val nextByte = inputByteArray[curBytePos++].toInt()
        resetBufferIfNeeded()
        return nextByte
    }

    private fun unescapedInput(): String {
        val xmlInput = byteBuilder.toString()
        val unescapedXml = parseXml(xmlInput)

        val inputMessage = if (isReplScriptExecuting)
            IdeLinebreaksUnescaper.unescapeFromDiez(unescapedXml)
        else
            "$unescapedXml$END_LINE"

        return inputMessage
    }

    private fun resetBufferIfNeeded() {
        if (isAtBufferEnd) {
            isXmlIncomplete = true
            byteBuilder = ByteArrayOutputStream()
            curBytePos = 0

            if (isReplScriptExecuting) isLastScriptByteProcessed = true
        }
    }
}