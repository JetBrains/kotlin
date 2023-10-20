/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.components

import org.jetbrains.kotlin.k1k2uicomparator.support.DefaultStyles
import java.awt.Font
import java.awt.Rectangle
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
    }
}
