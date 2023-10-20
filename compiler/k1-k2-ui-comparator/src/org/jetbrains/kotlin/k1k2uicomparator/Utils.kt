/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

import java.awt.*
import javax.swing.*

val JViewport.scrollableHeight get() = viewSize.height - extentSize.height
val JViewport.scrollableWidth get() = viewSize.width - extentSize.width

fun <T : JFrame> spawn(construct: () -> T) =
    construct().apply {
        setLocationRelativeTo(null)
        isVisible = true
    }

fun Component.withTitle(title: String) = JPanel().apply {
    layout = GridBagLayout()

    add(JLabel(title, SwingConstants.CENTER), GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        gridx = 0
        gridy = 0
        weightx = 1.0
        weighty = 0.0
        insets = Insets(0, 0, UIComparatorFrame.PANES_GAP, 0)
    })
    add(this@withTitle, GridBagConstraints().apply {
        fill = GridBagConstraints.BOTH
        gridx = 0
        gridy = 1
        weightx = 1.0
        weighty = 1.0
    })

    background = Color(0, 0, 0, 0)
    border = null
}
