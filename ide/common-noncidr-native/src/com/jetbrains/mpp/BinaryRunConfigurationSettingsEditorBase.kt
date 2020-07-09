/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.PanelWithAnchor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.konan.KonanBundle
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BinaryRunConfigurationSettingsEditorBase<T : BinaryRunConfigurationBase> :
    SettingsEditor<T>(),
    PanelWithAnchor {

    private val commonProgramParameters = CommonProgramParametersPanel()
    private val executableLabel: JBLabel = JBLabel(KonanBundle.message("label.executable.text") + ":")
    private val executableCombo: JComboBox<String> = ComboBox()

    private fun availableExecutables() = getWorkspace().executables

    private var anchor: JComponent? = null

    protected abstract fun getWorkspace(): WorkspaceBase

    private fun setupExecutableCombo(panel: JPanel, gridBag: GridBag) {
        availableExecutables().toSortedSet().forEach { executableCombo.addItem(it.base.name) }

        panel.add(executableLabel, gridBag.nextLine().next())
        panel.add(executableCombo, gridBag.next().coverLine())
        executableLabel.labelFor = executableCombo
    }

    private fun createEditorInner(panel: JPanel, gridBag: GridBag) {
        setupExecutableCombo(panel, gridBag)
        setupCommonProgramParametersPanel(panel, gridBag)
    }

    private fun setupCommonProgramParametersPanel(panel: JPanel, gridBag: GridBag) {
        panel.add(
            commonProgramParameters,
            gridBag.nextLine().weighty(1.0).insets(UIUtil.DEFAULT_VGAP * 2, -1, UIUtil.DEFAULT_VGAP * 2, -1).coverLine()
        )
    }

    override fun getAnchor(): JComponent? = anchor

    override fun setAnchor(anchor: JComponent?) {
        this.anchor = anchor
        executableLabel.anchor = anchor
        commonProgramParameters.anchor = anchor
    }

    override fun applyEditorTo(runConfiguration: T) {
        availableExecutables().firstOrNull { it.base.name == executableCombo.selectedItem }?.let {
            runConfiguration.executable = it
        }

        commonProgramParameters.applyTo(runConfiguration)
    }

    override fun resetEditorFrom(runConfiguration: T) {
        commonProgramParameters.reset(runConfiguration)
        executableCombo.selectedItem = runConfiguration.executable?.base?.name
    }

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())

        val gridBag = GridBag()
            .setDefaultFill(GridBagConstraints.BOTH)
            .setDefaultAnchor(GridBagConstraints.CENTER)
            .setDefaultWeightX(1, 1.0)
            .setDefaultInsets(0, JBUI.insets(0, 0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP))
            .setDefaultInsets(1, JBUI.insetsBottom(UIUtil.DEFAULT_VGAP))

        createEditorInner(panel, gridBag)
        setAnchor(commonProgramParameters.anchor)
        return panel
    }
}