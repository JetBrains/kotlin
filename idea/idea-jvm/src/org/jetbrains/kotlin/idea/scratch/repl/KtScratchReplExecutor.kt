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

package org.jetbrains.kotlin.idea.scratch.repl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.SOURCE_CHARS
import org.jetbrains.kotlin.console.XML_REPLACEMENTS
import org.jetbrains.kotlin.console.actions.logError
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputType
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory

private val XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

class KtScratchReplExecutor(file: ScratchFile) : ScratchExecutor(file) {
    private val history: ReplHistory = ReplHistory()
    private lateinit var osProcessHandler: OSProcessHandler

    override fun execute() {
        val module = file.getModule() ?: return error(file, "Module should be selected")
        val cmdLine = KotlinConsoleKeeper.createCommandLine(module)

        LOG.printDebugMessage("Execute REPL: ${cmdLine.commandLineString}")

        osProcessHandler = ReplOSProcessHandler(cmdLine)
        osProcessHandler.startNotify()

        file.getExpressions().forEach { expression ->
            history.addEntry(expression)
            sendCommandToProcess(expression.element.text)
        }

        sendCommandToProcess(":quit")
    }

    private fun sendCommandToProcess(command: String) {
        LOG.printDebugMessage("Send to REPL: ${command}")

        val processInputOS = osProcessHandler.processInput ?: return logError(this::class.java, "<p>Broken execute stream</p>")
        val charset = osProcessHandler.charset ?: Charsets.UTF_8

        val xmlRes = XML_PREAMBLE +
                "<input>" +
                StringUtil.escapeXml(
                    StringUtil.replace(command, SOURCE_CHARS, XML_REPLACEMENTS)
                ) +
                "</input>"

        val bytes = ("$xmlRes\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
    }

    private fun error(file: ScratchFile, message: String) {
        handlers.forEach { it.error(file, message) }
        handlers.forEach { it.onFinish(file) }
    }

    private class ReplHistory {
        private var entries = arrayListOf<ScratchExpression>()
        private var processedEntriesCount: Int = 0

        fun addEntry(entry: ScratchExpression) {
            entries.add(entry)
        }

        fun lastUnprocessedEntry(): ScratchExpression? {
            return entries.takeIf { processedEntriesCount < entries.size }?.get(processedEntriesCount)
        }

        fun lastProcessedEntry(): ScratchExpression? {
            val lastProcessedEntryIndex = processedEntriesCount - 1
            return entries.takeIf { lastProcessedEntryIndex < entries.size }?.get(lastProcessedEntryIndex)
        }

        fun entryProcessed() {
            processedEntriesCount++
        }
    }

    private inner class ReplOSProcessHandler(cmd: GeneralCommandLine) : OSProcessHandler(cmd) {
        private val factory = DocumentBuilderFactory.newInstance()

        override fun notifyTextAvailable(text: String, outputType: Key<*>) {
            if (text.startsWith("warning: classpath entry points to a non-existent location")) return

            if (outputType == ProcessOutputTypes.STDOUT) {
                handleReplMessage(text)
            }
        }

        override fun notifyProcessTerminated(exitCode: Int) {
            handlers.forEach { it.onFinish(file) }
        }

        private fun strToSource(s: String, encoding: Charset = Charsets.UTF_8) = InputSource(ByteArrayInputStream(s.toByteArray(encoding)))

        private fun handleReplMessage(text: String) {
            if (text.isBlank()) return
            val output = try {
                factory.newDocumentBuilder().parse(strToSource(text))
            } catch (e: Exception) {
                handlers.forEach { it.error(file, "Couldn't parse REPL output: $text") }
                return
            }

            val root = output.firstChild as Element
            val outputType = root.getAttribute("type")
            val content = StringUtil.replace(root.textContent, XML_REPLACEMENTS, SOURCE_CHARS).trim('\n')

            LOG.printDebugMessage("REPL output: $outputType $content")

            if (outputType in setOf("SUCCESS", "COMPILE_ERROR", "INTERNAL_ERROR", "RUNTIME_ERROR", "READLINE_END")) {
                history.entryProcessed()
            }

            val result = parseReplOutput(content, outputType)
            if (result != null) {
                val lastExpression = if (outputType == "USER_OUTPUT") {
                    // success command is printed after user output
                    history.lastUnprocessedEntry()
                } else {
                    history.lastProcessedEntry()
                }

                if (lastExpression != null) {
                    handlers.forEach { it.handle(file, lastExpression, result) }
                }
            }
        }

        private fun parseReplOutput(text: String, outputType: String): ScratchOutput? {
            return when (outputType) {
                "USER_OUTPUT" -> ScratchOutput(text, ScratchOutputType.OUTPUT)
                "REPL_RESULT" -> ScratchOutput(text, ScratchOutputType.RESULT)
                "REPL_INCOMPLETE",
                "INTERNAL_ERROR",
                "COMPILE_ERROR",
                "RUNTIME_ERROR" -> ScratchOutput(text, ScratchOutputType.ERROR)
                else -> null
            }
        }
    }
}