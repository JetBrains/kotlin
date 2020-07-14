/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.runconfig

import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.PanelWithAnchor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.konan.KonanBundle
import com.jetbrains.mpp.KonanExecutable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class BinaryRunConfigurationSettingsEditor(
    availableExecutables: Set<KonanExecutable>
) : SettingsEditor<BinaryRunConfiguration>(),
    PanelWithAnchor {
    private val availableExecutableItems = availableExecutables.map { KonanExecutableItem(it) }

    private val commonProgramParameters = CommonProgramParametersPanel()
    private val executableLabel: JBLabel = JBLabel(KonanBundle.message("label.executable.text") + ":")
    private val executableCombo = ComboBox<KonanExecutableItem>()

    private var anchor: JComponent? = null
    override fun getAnchor(): JComponent? = anchor
    override fun setAnchor(component: JComponent?) {
        anchor = component
        executableLabel.anchor = component
        commonProgramParameters.anchor = component
    }

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gridBag = GridBag()
            .setDefaultFill(GridBagConstraints.BOTH)
            .setDefaultAnchor(GridBagConstraints.CENTER)
            .setDefaultWeightX(1, 1.0)
            .setDefaultInsets(0, JBUI.insets(0, 0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP))
            .setDefaultInsets(1, JBUI.insetsBottom(UIUtil.DEFAULT_VGAP))


        availableExecutableItems.forEach { executableCombo.addItem(it) }
        panel.add(executableLabel, gridBag.nextLine().next())
        panel.add(executableCombo, gridBag.next().coverLine())
        executableLabel.labelFor = executableCombo

        panel.add(
            commonProgramParameters,
            gridBag.nextLine()
                .weighty(1.0)
                .insets(UIUtil.DEFAULT_VGAP * 2, -1, UIUtil.DEFAULT_VGAP * 2, -1)
                .coverLine()
        )
        setAnchor(commonProgramParameters.anchor)
        return panel
    }

    override fun applyEditorTo(runConfiguration: BinaryRunConfiguration) {
        runConfiguration.executable = (executableCombo.selectedItem as? KonanExecutableItem)?.exec
        commonProgramParameters.applyTo(runConfiguration)
    }

    override fun resetEditorFrom(runConfiguration: BinaryRunConfiguration) {
        commonProgramParameters.reset(runConfiguration)
        executableCombo.selectedItem = runConfiguration.executable
    }

    private class KonanExecutableItem(
        val exec: KonanExecutable
    ) {
        override fun toString() = exec.base.name
    }
}