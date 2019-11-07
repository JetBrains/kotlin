/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

class InlayScratchFileRenderer(val text: String, private val outputType: ScratchOutputType) : EditorCustomElementRenderer {
    private fun getFontInfo(editor: Editor): FontInfo {
        val colorsScheme = editor.colorsScheme
        val fontPreferences = colorsScheme.fontPreferences
        val attributes = getAttributesForOutputType(outputType)
        val fontStyle = attributes.fontType
        return ComplementaryFontsRegistry.getFontAbleToDisplay(
            'a'.toInt(), fontStyle, fontPreferences, FontInfo.getFontRenderContext(editor.contentComponent)
        )
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val fontInfo = getFontInfo(inlay.editor)
        return fontInfo.fontMetrics().stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val attributes = getAttributesForOutputType(outputType)
        val fgColor = attributes.foregroundColor ?: return
        g.color = fgColor
        val fontInfo = getFontInfo(inlay.editor)
        g.font = fontInfo.font
        val metrics = fontInfo.fontMetrics()
        g.drawString(text, targetRegion.x, targetRegion.y + metrics.ascent)
    }

    override fun toString(): String {
        return "${text.takeWhile { it.isWhitespace() }}${outputType.name}: ${text.trim()}"
    }
}
