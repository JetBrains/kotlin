/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.ui.OptionGroup
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JPanel

class KotlinOtherSettingsPanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(KotlinLanguage.INSTANCE, null, settings) {
    private val cbTrailingComma = JCheckBox(KotlinBundle.message("formatter.checkbox.text.use.trailing.comma"))

    override fun getRightMargin() = throw UnsupportedOperationException()

    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()

    override fun getFileType() = throw UnsupportedOperationException()

    override fun getPreviewText(): String? = null

    override fun apply(settings: CodeStyleSettings) {
        settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA = cbTrailingComma.isSelected
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA != cbTrailingComma.isSelected
    }

    override fun getPanel() = jPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        cbTrailingComma.isSelected = settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA
    }

    override fun getTabTitle(): String = KotlinBundle.message("formatter.title.other")

    private val jPanel = JPanel(BorderLayout()).apply {
        add(
            JBScrollPane(
                JPanel(VerticalLayout(JBUI.scale(5))).apply {
                    border = BorderFactory.createEmptyBorder(UIUtil.DEFAULT_VGAP, 10, UIUtil.DEFAULT_VGAP, 10)
                    add(
                        OptionGroup(KotlinBundle.message("formatter.title.trailing.comma")).apply {
                            add(cbTrailingComma)
                        }.createPanel()
                    )
                }
            )
        )
    }
}