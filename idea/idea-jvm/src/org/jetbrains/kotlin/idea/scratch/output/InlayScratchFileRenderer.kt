/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.Colors
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

class InlayScratchFileRenderer(val text: String, private val outputType: ScratchOutputType) : EditorCustomElementRenderer {
    private fun getFontInfo(editor: Editor): FontInfo {
        val colorsScheme = editor.colorsScheme
        val fontPreferences = colorsScheme.fontPreferences
        val attributes = getAttributes()
        val fontStyle = attributes.fontType
        return ComplementaryFontsRegistry.getFontAbleToDisplay(
            'a'.toInt(), fontStyle, fontPreferences, FontInfo.getFontRenderContext(editor.contentComponent)
        )
    }

    override fun calcWidthInPixels(editor: Editor): Int {
        val fontInfo = getFontInfo(editor)
        return fontInfo.fontMetrics().stringWidth(text)
    }

    override fun paint(editor: Editor, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val attributes = getAttributes()
        val fgColor = attributes.foregroundColor ?: return
        g.color = fgColor
        val fontInfo = getFontInfo(editor)
        g.font = fontInfo.font
        val metrics = fontInfo.fontMetrics()
        g.drawString(text, r.x, r.y + metrics.ascent)
    }

    private fun getAttributes(): TextAttributes {
        return when (outputType) {
            ScratchOutputType.OUTPUT -> userOutputAttributes
            ScratchOutputType.RESULT -> normalAttributes
            ScratchOutputType.ERROR -> errorAttributes
        }
    }

    override fun toString(): String {
        return "${outputType.name}: ${text.trim()}"
    }

    companion object {
        private val normalAttributes = TextAttributes(
            JBColor.GRAY,
            null, null, null,
            Font.ITALIC
        )

        private val errorAttributes = TextAttributes(
            JBColor(Colors.DARK_RED, Colors.DARK_RED),
            null, null, null,
            Font.ITALIC
        )

        private val userOutputColor = Color(0x5C5CFF)
        private val userOutputAttributes = TextAttributes(
            JBColor(userOutputColor, userOutputColor),
            null, null, null,
            Font.ITALIC
        )
    }
}