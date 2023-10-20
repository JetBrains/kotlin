/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

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

fun JTextArea.styleAsCodeEditor() {
    font = Font(Font.MONOSPACED, Font.PLAIN, 14)
    tabSize = 4
}

fun codeEditorArea(text: String? = null) = JTextArea(text).apply { styleAsCodeEditor() }
