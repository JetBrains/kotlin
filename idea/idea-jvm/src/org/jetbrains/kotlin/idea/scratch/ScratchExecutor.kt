/*
 * Copyrig()ht 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandler

abstract class ScratchExecutor(protected val file: ScratchFile) {
    abstract fun execute()
    abstract fun stop()

    protected val handler = CompositeOutputHandler()

    fun addOutputHandler(outputHandler: ScratchOutputHandler) {
        handler.add(outputHandler)
    }

    fun errorOccurs(message: String, e: Throwable? = null, isFatal: Boolean = false) {
        handler.error(file, message)

        if (isFatal) {
            handler.onFinish(file)
        }

        if (e != null) LOG.error(e)
    }

    class CompositeOutputHandler : ScratchOutputHandler {
        private val handlers = mutableSetOf<ScratchOutputHandler>()

        fun add(handler: ScratchOutputHandler) {
            handlers.add(handler)
        }

        fun remove(handler: ScratchOutputHandler) {
            handlers.remove(handler)
        }

        override fun onStart(file: ScratchFile) {
            handlers.forEach { it.onStart(file) }
        }

        override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
            handlers.forEach { it.handle(file, expression, output) }
        }

        override fun error(file: ScratchFile, message: String) {
            handlers.forEach { it.error(file, message) }
        }

        override fun onFinish(file: ScratchFile) {
            handlers.forEach { it.onFinish(file) }
        }

        override fun clear(file: ScratchFile) {
            handlers.forEach { it.clear(file) }
        }
    }
}

abstract class SequentialScratchExecutor(file: ScratchFile) : ScratchExecutor(file) {
    abstract fun executeStatement(expression: ScratchExpression)

    protected abstract fun startExecution()
    protected abstract fun stopExecution(callback: (() -> Unit)? = null)

    protected abstract fun needProcessToStart(): Boolean

    private var lastExecuted = 0

    fun start() {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(listener, file.project.messageBus.connect())

        startExecution()
    }

    override fun stop() {
        EditorFactory.getInstance().eventMulticaster.removeDocumentListener(listener)

        stopExecution()
    }

    fun executeNew() {
        handler.onStart(file)

        for ((index, expression) in file.getExpressions().withIndex()) {
            if (index + 1 <= lastExecuted) continue
            executeStatement(expression)
            lastExecuted = index + 1
        }
    }

    override fun execute() {
        if (needToRestartProcess()) {
            lastExecuted = 0
            handler.clear(file)

            handler.onStart(file)
            stopExecution {
                ApplicationManager.getApplication().invokeLater {
                    executeNew()
                }
            }
        } else {
            executeNew()
        }
    }

    fun getFirstNewExpression(): ScratchExpression? {
        val expressions = runReadAction { file.getExpressions() }
        if (lastExecuted in expressions.indices) {
            return expressions[lastExecuted]
        }
        return null
    }

    private fun needToRestartProcess(): Boolean {
        return lastExecuted > 0
    }

    private val listener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            if (event.newFragment.isBlank() && event.oldFragment.isBlank()) return
            if (!needToRestartProcess()) return

            val document = event.document
            val virtualFile = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return
            if (!virtualFile.isValid) {
                return
            }

            if (PsiManager.getInstance(file.project).findFile(virtualFile) != file.getPsiFile()) return

            val changedLine = document.getLineNumber(event.offset)
            val changedExpression = file.getExpressionAtLine(changedLine) ?: return
            val changedExpressionIndex = file.getExpressions().indexOf(changedExpression) + 1
            if (changedExpressionIndex <= lastExecuted) {
                lastExecuted = 0
                handler.clear(file)

                stopExecution()
            }
        }
    }
}