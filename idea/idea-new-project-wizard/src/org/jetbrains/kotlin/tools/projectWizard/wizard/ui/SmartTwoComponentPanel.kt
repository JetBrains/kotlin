/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Spring
import javax.swing.SpringLayout

class SmartTwoComponentPanel(
    main: JComponent,
    side: JComponent,
    sideIsOnTheRight: Boolean
) : JPanel(SpringLayout()) {
    private val springLayout = this@SmartTwoComponentPanel.layout as SpringLayout
    private fun JComponent.constraints() = springLayout.getConstraints(this)

    init {
        add(main)
        add(side)

        val left = if (sideIsOnTheRight) main else side
        val right = if (sideIsOnTheRight) side else main

        springLayout.putConstraint(SpringLayout.WEST, left, 0, SpringLayout.WEST, this)
        springLayout.putConstraint(SpringLayout.NORTH, left, 0, SpringLayout.NORTH, this)

        springLayout.putConstraint(SpringLayout.WEST, right, 0, SpringLayout.EAST, left)
        springLayout.putConstraint(SpringLayout.NORTH, right, 0, SpringLayout.NORTH, this)

        springLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, right)
        springLayout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, right)
        springLayout.putConstraint(SpringLayout.SOUTH, left, 0, SpringLayout.SOUTH, this)

        val mainConstraints = main.constraints()
        val sideConstraints = side.constraints()

        mainConstraints.width = Spring.max(mainConstraints.width, MAIN_PANEL_MIN_WIDTH.asSpring())
        sideConstraints.width = springMin(sideConstraints.width, SIDE_PANEL_MAX_WIDTH.asSpring())
    }

    companion object {
        private const val MAIN_PANEL_MIN_WIDTH = 500
        private const val SIDE_PANEL_MAX_WIDTH = 280
    }
}