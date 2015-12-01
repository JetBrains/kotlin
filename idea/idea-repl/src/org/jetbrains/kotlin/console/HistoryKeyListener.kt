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

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class HistoryKeyListener(
        private val project: Project, private val consoleEditor: EditorEx, private val history: CommandHistory
) : KeyAdapter(), HistoryUpdateListener {
    private var historyPos = 0
    private var prevCaretOffset = -1
    private var unfinishedCommand = ""

    override fun onNewEntry(entry: CommandHistory.Entry) {
        // reset history positions
        historyPos = history.size
        prevCaretOffset = -1
        unfinishedCommand = ""
    }

    private enum class HistoryMove {
        UP, DOWN
    }

    override fun keyReleased(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_UP -> moveHistoryCursor(HistoryMove.UP)
            KeyEvent.VK_DOWN -> moveHistoryCursor(HistoryMove.DOWN)
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> prevCaretOffset = consoleEditor.caretModel.offset
        }
    }

    private fun moveHistoryCursor(move: HistoryMove) {
        if (history.size == 0) return
        if (LookupManager.getInstance(project).activeLookup != null) return

        val caret = consoleEditor.caretModel
        val document = consoleEditor.document

        val curOffset = caret.offset
        val curLine = document.getLineNumber(curOffset)
        val totalLines = document.lineCount
        val isMultiline = totalLines > 1

        when (move) {
            HistoryMove.UP -> {
                if (curLine != 0 || (isMultiline && prevCaretOffset != 0)) {
                    prevCaretOffset = curOffset
                    return
                }

                if (historyPos == history.size) {
                    unfinishedCommand = document.text
                }

                historyPos = Math.max(historyPos - 1, 0)
                WriteCommandAction.runWriteCommandAction(project) {
                    document.setText(history[historyPos].entryText)
                    EditorUtil.scrollToTheEnd(consoleEditor)
                    prevCaretOffset = 0
                    caret.moveToOffset(0)
                }
            }
            HistoryMove.DOWN -> {
                if (historyPos == history.size) return

                if (curLine != totalLines - 1 || (isMultiline && prevCaretOffset != document.textLength)) {
                    prevCaretOffset = curOffset
                    return
                }

                historyPos = Math.min(historyPos + 1, history.size)
                WriteCommandAction.runWriteCommandAction(project) {
                    document.setText(if (historyPos == history.size) unfinishedCommand else history[historyPos].entryText)
                    prevCaretOffset = document.textLength
                    EditorUtil.scrollToTheEnd(consoleEditor)
                }
            }
        }
    }
}