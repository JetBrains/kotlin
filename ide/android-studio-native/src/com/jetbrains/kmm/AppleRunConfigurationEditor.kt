/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.AppleRunConfiguration
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class AppleRunConfigurationEditor(project: Project) : SettingsEditor<AppleRunConfiguration>() {
    override fun resetEditorFrom(configuration: AppleRunConfiguration) {
    }

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())
        return panel
    }

    override fun applyEditorTo(configuration: AppleRunConfiguration) {
    }
}
