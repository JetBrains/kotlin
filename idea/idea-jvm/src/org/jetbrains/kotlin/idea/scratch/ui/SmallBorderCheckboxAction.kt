/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.ui

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

abstract class SmallBorderCheckboxAction(text: String, description: String? = null) : CheckboxAction(text, description, null) {
    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val checkbox = super.createCustomComponent(presentation, place)
        checkbox.border = JBUI.Borders.emptyRight(4)
        return checkbox
    }
}