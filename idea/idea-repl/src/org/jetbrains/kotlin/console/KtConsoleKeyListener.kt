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

import com.intellij.openapi.command.WriteCommandAction
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

public class KtConsoleKeyListener(private val ktConsole: KotlinConsoleRunner) : KeyAdapter() {
    private var historyPos = 0
    private var prevCaretOffset = -1
    private var unfinishedCommand = ""

    private enum class HistoryMove {
        UP, DOWN
    }

    public fun resetHistoryPosition() {
        historyPos = ktConsole.history.size()
        prevCaretOffset = -1
        unfinishedCommand = ""
    }

    override fun keyReleased(e: KeyEvent): Unit = when (e.keyCode) {
        KeyEvent.VK_UP    -> moveHistoryCursor(HistoryMove.UP)
        KeyEvent.VK_DOWN  -> moveHistoryCursor(HistoryMove.DOWN)
    }

    private fun moveHistoryCursor(move: HistoryMove) {
        val history = ktConsole.history
        if (history.isEmpty()) return

        val caret = ktConsole.consoleView.consoleEditor.caretModel
        val document = ktConsole.consoleView.editorDocument

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

                if (historyPos == history.size()) {
                    unfinishedCommand = document.text
                }

                historyPos = Math.max(historyPos - 1, 0)
                WriteCommandAction.runWriteCommandAction(ktConsole.project) {
                    document.setText(history[historyPos])
                    caret.moveToOffset(0)
                    prevCaretOffset = 0
                }
            }
            HistoryMove.DOWN -> {
                if (curLine != totalLines - 1 || (isMultiline && prevCaretOffset != document.textLength)) {
                    prevCaretOffset = curOffset
                    return
                }

                historyPos = Math.min(historyPos + 1, history.size())
                WriteCommandAction.runWriteCommandAction(ktConsole.project) {
                    document.setText(if (historyPos == history.size()) unfinishedCommand else history[historyPos])
                    caret.moveToOffset(document.textLength)
                    prevCaretOffset = document.textLength
                }
            }
        }
    }
}