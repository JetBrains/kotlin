/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.SwingConstants

class CommentLabel(text: String? = null) : JBLabel() {
    init {
        if (text != null) {
            this.text = text
        }
        verticalAlignment = SwingConstants.TOP
        isFocusable = false
        foreground = UIUtil.getContextHelpForeground()

        // taken from com.intellij.openapi.ui.panel.ComponentPanelBuilder.createCommentComponent
        if (SystemInfo.isMac) {
            val font = this.font
            val size = font.size2D
            val smallFont = font.deriveFont(size - 2.0f)
            this.font = smallFont
        }
    }
}

fun commentLabel(text: String, init: JBLabel.() -> Unit = {}) =
    CommentLabel(text).apply(init)

fun componentWithCommentAtRight(component: JComponent, label: String?) = borderPanel {
    addToLeft(component)
    label?.let {
        addToCenter(commentLabel(it) {
            withBorder(JBUI.Borders.emptyLeft(5))
            verticalAlignment = SwingConstants.CENTER
        })
    }
}

fun componentWithCommentAtBottom(component: JComponent, label: String?) = borderPanel {
    addToTop(component)
    label?.let {
        addToCenter(commentLabel(it) {
            withBorder(JBUI.Borders.emptyLeft(4))
        })
    }
}