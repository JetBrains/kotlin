/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.GridBag
import org.jetbrains.konan.execution.*
import java.io.File
import javax.swing.JLabel
import javax.swing.JPanel

class MobileTestRunConfigurationEditor(project: Project, helper: MobileBuildConfigurationHelper) :
    MobileRunConfigurationEditor(project, helper) {

    private lateinit var testBundlePathField: JBTextField

    override fun createAdditionalControls(panel: JPanel, g: GridBag) {
        super.createAdditionalControls(panel, g)

        testBundlePathField = JBTextField()
        val label = JLabel("Test bundle:")
        panel.add(label, g.nextLine().next())
        panel.add(testBundlePathField, g.next().coverLine())
        label.labelFor = testBundlePathField
    }

    override fun applyEditorTo(runConfiguration: MobileRunConfiguration) {
        super.applyEditorTo(runConfiguration)
        (runConfiguration as MobileTestRunConfiguration).testRunner = File(testBundlePathField.text)
    }

    override fun resetEditorFrom(runConfiguration: MobileRunConfiguration) {
        super.resetEditorFrom(runConfiguration)
        testBundlePathField.text = (runConfiguration as MobileTestRunConfiguration).testRunner.path
    }
}