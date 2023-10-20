/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.components

import org.jetbrains.kotlin.k1k2uicomparator.support.DefaultStyles
import org.jetbrains.kotlin.k1k2uicomparator.support.alignByMultiplesOf
import org.jetbrains.kotlin.k1k2uicomparator.support.indentWidth
import org.jetbrains.kotlin.k1k2uicomparator.support.isOdd
import java.awt.Font
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextArea
import javax.swing.text.DefaultCaret

// Without it the read-only text areas
// will be jumping to the bottom on
// text updates.
class CaretWithoutVisibilityAdjustment : DefaultCaret() {
    override fun adjustVisibility(nloc: Rectangle?) {}
}

data class CodeAreaStyle(
    val font: Font = Font(Font.MONOSPACED, Font.PLAIN, DefaultStyles.DEFAULT_FONT_SIZE),
    val padding: Int = DefaultStyles.DEFAULT_GAP,
    val tabSize: Int = 4,
)

fun codeArea(
    text: String? = DefaultStyles.DEFAULT_SOURCE,
    style: CodeAreaStyle = CodeAreaStyle(),
    readonly: Boolean = false,
) = JTextArea(text).apply {
    font = style.font
    border = emptyBorderWithEqualGaps(style.padding)
    tabSize = style.tabSize

    if (readonly) {
        isEditable = false
        caret = CaretWithoutVisibilityAdjustment()
        return@apply
    }

    addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {
            e?.let(::handleConsumingCodeAreaInput)
        }

        override fun keyPressed(e: KeyEvent?) {
            e?.let(::handleDefaultCodeAreaInput)
        }

        override fun keyReleased(e: KeyEvent?) {}
    })
}

private val pairedSymbol = mapOf(
    '(' to ')',
    '[' to ']',
    '{' to '}',
    '`' to '`',
    '"' to '"',
    '\'' to '\'',
)

private val Char.fullPair get() = "$this" + pairedSymbol[this]

private val indentShiftingCharacters = pairedSymbol.entries.take(3).associate { it.key to it.value }
private val quotes = listOf('`', '"', '\'')

private val JTextArea.previousCharacter get() = getText(caretPosition - 1, 1).first()
private val JTextArea.nextCharacter get() = getText(caretPosition, 1).first()

private val JTextArea.previousIsFirstInPair get() = caretPosition > 0 && previousCharacter in pairedSymbol
private val JTextArea.nextIsSecondInPair get() = caretPosition < text.length && nextCharacter == pairedSymbol[previousCharacter]

private fun JTextArea.getIndentOfLine(line: String): Int {
    val noTabLine = line.removeSuffix("\n").replace("\t", " ".repeat(tabSize))
    return noTabLine.indentWidth().alignByMultiplesOf(tabSize)
}

private fun JTextArea.getIndentOfLineWithin(lineStartOffset: Int, lineEndOffset: Int) =
    getIndentOfLine(text.substring(lineStartOffset, lineEndOffset))

private val JTextArea.isLinePartBeforeCaretBlank: Boolean
    get() {
        val currentLineNumber = getLineOfOffset(caretPosition)
        val lineStartOffset = getLineStartOffset(currentLineNumber)
        val lineBeforeCaret = text.substring(lineStartOffset, caretPosition)
        return lineBeforeCaret.isBlank()
    }

private fun JTextArea.isProbablyFixingUnbalancedQuote(character: Char): Boolean {
    if (character !in quotes) {
        return false
    }

    val currentLineNumber = getLineOfOffset(caretPosition)
    val currentLineStartOffset = getLineStartOffset(currentLineNumber)
    return text.substring(currentLineStartOffset, caretPosition).count { it == character }.isOdd
}

/**
 * Sometimes, consuming events in keyPressed doesn't
 * work, so they are moved from this "default" handler
 * to the "consuming" handler below.
 */
private fun JTextArea.handleDefaultCodeAreaInput(e: KeyEvent) {
    if (this.selectionStart != this.selectionEnd) {
        return
    }

    when (e.extendedKeyCode) {
        KeyEvent.VK_BACK_SPACE -> when {
            previousIsFirstInPair && nextIsSecondInPair -> {
                replaceRange("", caretPosition - 1, caretPosition + 1)
                e.consume()
            }
            isLinePartBeforeCaretBlank -> {
                val currentLineNumber = getLineOfOffset(caretPosition)
                val currentLineStartOffset = getLineStartOffset(currentLineNumber)
                val currentLineEndOffset = getLineEndOffset(currentLineNumber)
                val currentLineIndent = getIndentOfLineWithin(currentLineStartOffset, currentLineEndOffset)

                if (currentLineNumber > 0) {
                    val previousLineStart = getLineStartOffset(currentLineNumber - 1)
                    val previousLine = text.substring(previousLineStart, currentLineStartOffset)

                    if (previousLine.isNotBlank()) {
                        val inverseIndent = previousLine.reversed().indentWidth()
                        replaceRange("", currentLineStartOffset - inverseIndent, currentLineStartOffset + currentLineIndent)
                    } else {
                        replaceRange(" ".repeat(currentLineIndent), previousLineStart, currentLineStartOffset + currentLineIndent)
                    }
                } else {
                    replaceRange("", currentLineStartOffset, currentLineStartOffset + currentLineIndent)
                }

                e.consume()
            }
        }
        KeyEvent.VK_TAB -> {
            insert(" ".repeat(tabSize), caretPosition)
            e.consume()
        }
        KeyEvent.VK_ENTER -> {
            val currentLineNumber = getLineOfOffset(caretPosition)
            val currentLineStartOffset = getLineStartOffset(currentLineNumber)
            val currentLineEndOffset = getLineEndOffset(currentLineNumber)
            val currentLineIndent = getIndentOfLineWithin(currentLineStartOffset, currentLineEndOffset)
            val indentAfterCaret = getIndentOfLineWithin(caretPosition, currentLineEndOffset)

            when {
                caretPosition > 0 && previousCharacter in indentShiftingCharacters.keys -> {
                    replaceRange("\n" + " ".repeat(currentLineIndent + tabSize), caretPosition, caretPosition + indentAfterCaret)

                    if (caretPosition < text.length && nextCharacter in indentShiftingCharacters.values) {
                        val spaceAfterCaret = "\n" + " ".repeat(currentLineIndent)
                        insert(spaceAfterCaret, caretPosition)
                        caretPosition -= spaceAfterCaret.length
                    }
                }
                else -> {
                    val spaceBeforeCaret = "\n" + " ".repeat(currentLineIndent)
                    replaceRange(spaceBeforeCaret, caretPosition, caretPosition + indentAfterCaret)
                }
            }

            e.consume()
        }
    }
}

/**
 * See [org.jetbrains.kotlin.k1k2uicomparator.components.handleDefaultCodeAreaInput].
 */
private fun JTextArea.handleConsumingCodeAreaInput(e: KeyEvent) {
    if (this.selectionStart != this.selectionEnd) {
        return
    }

    when {
        e.keyChar in pairedSymbol.values && caretPosition < text.length && nextCharacter == e.keyChar -> {
            caretPosition++
            e.consume()
        }
        e.keyChar in pairedSymbol && !isProbablyFixingUnbalancedQuote(e.keyChar) -> {
            insert(e.keyChar.fullPair, caretPosition)
            caretPosition--
            e.consume()
        }
    }
}
