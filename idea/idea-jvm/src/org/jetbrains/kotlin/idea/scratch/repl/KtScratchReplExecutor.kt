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
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml
import org.jetbrains.kotlin.cli.common.repl.replNormalizeLineBreaks
import org.jetbrains.kotlin.cli.common.repl.replRemoveLineBreaksInTheEnd
import org.jetbrains.kotlin.cli.common.repl.replUnescapeLineBreaks
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.actions.logError
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputType
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory

class KtScratchReplExecutor(file: ScratchFile) : SequentialScratchExecutor(file) {
    private val history: ReplHistory = ReplHistory()
    private var osProcessHandler: OSProcessHandler? = null

    override fun startExecution() {
        val module = file.module
        val cmdLine = KotlinConsoleKeeper.createReplCommandLine(file.project, module)

        LOG.printDebugMessage("Execute REPL: ${cmdLine.commandLineString}")

        osProcessHandler = ReplOSProcessHandler(cmdLine)
        osProcessHandler?.startNotify()
    }

    override fun stopExecution(callback: (() -> Unit)?) {
        val osProcessHandler = osProcessHandler ?: return

        try {
            if (callback != null) {
                osProcessHandler.addProcessListener(object : ProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        callback()
                    }
                })
            }
            sendCommandToProcess(":quit")
        } catch (e: Exception) {
            errorOccurs("Couldn't stop REPL process", e, false)

            osProcessHandler.destroyProcess()
            clearState()
        }
    }

    private fun clearState() {
        history.clear()
        osProcessHandler = null
        handler.onFinish(file)
    }

    override fun executeStatement(expression: ScratchExpression) {
        if (needProcessToStart()) {
            startExecution()
        }

        history.addEntry(expression)
        try {
            sendCommandToProcess(expression.element.text)
        } catch (e: Throwable) {
            errorOccurs("Couldn't execute statement: ${expression.element.text}", e, true)
        }
    }

    override fun needProcessToStart(): Boolean {
        return osProcessHandler == null
    }

    private fun sendCommandToProcess(command: String) {
        LOG.printDebugMessage("Send to REPL: ${command}")

        val processInputOS = osProcessHandler?.processInput ?: return logError(this::class.java, "<p>Broken execute stream</p>")
        val charset = osProcessHandler?.charset ?: Charsets.UTF_8

        val xmlRes = command.replInputAsXml()

        val bytes = ("$xmlRes\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
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
            if (processedEntriesCount < 1) return null

            val lastProcessedEntryIndex = processedEntriesCount - 1
            return entries.takeIf { lastProcessedEntryIndex < entries.size }?.get(lastProcessedEntryIndex)
        }

        fun entryProcessed() {
            processedEntriesCount++
        }

        fun clear() {
            entries = arrayListOf()
            processedEntriesCount = 0
        }

        fun isAllProcessed() = entries.size == processedEntriesCount
    }

    private inner class ReplOSProcessHandler(cmd: GeneralCommandLine) : OSProcessHandler(cmd) {
        private val factory = DocumentBuilderFactory.newInstance()

        override fun notifyTextAvailable(text: String, outputType: Key<*>) {
            if (text.startsWith("warning: classpath entry points to a non-existent location")) return

            when (outputType) {
                ProcessOutputTypes.STDOUT -> handleReplMessage(text)
                ProcessOutputTypes.STDERR -> errorOccurs(text)
            }
        }

        override fun notifyProcessTerminated(exitCode: Int) {
            super.notifyProcessTerminated(exitCode)

            clearState()
        }

        private fun strToSource(s: String, encoding: Charset = Charsets.UTF_8) = InputSource(ByteArrayInputStream(s.toByteArray(encoding)))

        private fun handleReplMessage(text: String) {
            if (text.isBlank()) return
            val output = try {
                factory.newDocumentBuilder().parse(strToSource(text))
            } catch (e: Exception) {
                return handler.error(file, "Couldn't parse REPL output: $text")
            }

            val root = output.firstChild as Element
            val outputType = root.getAttribute("type")
            val content = root.textContent.replUnescapeLineBreaks().replNormalizeLineBreaks().replRemoveLineBreaksInTheEnd()

            LOG.printDebugMessage("REPL output: $outputType $content")

            if (outputType in setOf("SUCCESS", "COMPILE_ERROR", "INTERNAL_ERROR", "RUNTIME_ERROR", "READLINE_END")) {
                history.entryProcessed()
                if (history.isAllProcessed()) {
                    handler.onFinish(file)
                }
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
                    handler.handle(file, lastExpression, result)
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