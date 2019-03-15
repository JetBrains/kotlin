/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.codeStyle.CommenterForm
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.DisplayPriority
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBInsets
import org.jetbrains.kotlin.idea.KotlinLanguage
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class KotlinGenerationSettingsProvider : CodeStyleSettingsProviderCompat() {
    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
        return KotlinCodeStyleGenerationConfigurable(settings)
    }

    override fun getConfigurableDisplayName(): String = ApplicationBundle.message("title.code.generation")
    override fun getPriority(): DisplayPriority = DisplayPriority.CODE_SETTINGS
    override fun hasSettingsPage() = false
    override fun getLanguage() = KotlinLanguage.INSTANCE
}

class KotlinCodeStyleGenerationConfigurable(private val mySettings: CodeStyleSettings) : CodeStyleConfigurable {
    private val myCommenterForm: CommenterForm = CommenterForm(KotlinLanguage.INSTANCE)

    override fun getDisplayName(): String = ApplicationBundle.message("title.code.generation")

    override fun createComponent(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = IdeBorderFactory.createEmptyBorder(JBInsets(0, 10, 10, 10))
            add(myCommenterForm.commenterPanel)
        }
    }

    override fun isModified(): Boolean {
        return myCommenterForm.isModified(mySettings)
    }

    override fun apply() {
        apply(mySettings)
    }

    override fun reset() {
        reset(mySettings)
    }

    override fun reset(settings: CodeStyleSettings) {
        myCommenterForm.reset(settings)
    }

    override fun apply(settings: CodeStyleSettings) {
        myCommenterForm.apply(settings)
    }
}
