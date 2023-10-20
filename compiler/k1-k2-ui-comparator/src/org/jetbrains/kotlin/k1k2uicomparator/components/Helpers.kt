/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.components

import org.jetbrains.kotlin.k1k2uicomparator.support.DefaultStyles
import org.jetbrains.kotlin.k1k2uicomparator.support.unaryPlus
import org.jetbrains.kotlin.k1k2uicomparator.support.with
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.Border

fun Component.withTitle(
    title: String,
    gap: Int = DefaultStyles.DEFAULT_GAP,
) = JPanel().apply {
    layout = GridBagLayout()

    +JLabel(title, SwingConstants.CENTER).with(GridBagConstraints()) {
        fill = GridBagConstraints.HORIZONTAL
        gridx = 0
        gridy = 0
        weightx = 1.0
        weighty = 0.0
        insets = Insets(0, 0, gap, 0)
    }
    +this@withTitle.with(GridBagConstraints()) {
        fill = GridBagConstraints.BOTH
        gridx = 0
        gridy = 1
        weightx = 1.0
        weighty = 1.0
    }

    isOpaque = false
    border = null
}

fun emptyBorderWithEqualGaps(gap: Int): Border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
