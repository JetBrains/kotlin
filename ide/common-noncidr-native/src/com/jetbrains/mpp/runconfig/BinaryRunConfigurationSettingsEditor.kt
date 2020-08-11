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
import com.jetbrains.mpp.BinaryExecutable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JPanel

class BinaryRunConfigurationSettingsEditor(
    availableExecutables: Set<BinaryExecutable>
) : SettingsEditor<BinaryRunConfiguration>(),
    PanelWithAnchor {
    private val availableExecutableItems = availableExecutables
        .filter { !it.isTest }
        .map { BinaryExecutableItem(it) }

    private val commonProgramParameters = CommonProgramParametersPanel()

    private val executableLabel: JBLabel = JBLabel("Executable:")
    private val executableCombo = ComboBox<BinaryExecutableItem>()

    private val variantLabel: JBLabel = JBLabel("Variant:")
    private val variantCombo = ComboBox<BinaryExecutableVariantItem>()

    private var anchor: JComponent? = null
    override fun getAnchor(): JComponent? = anchor
    override fun setAnchor(component: JComponent?) {
        anchor = component
        executableLabel.anchor = component
        variantLabel.anchor = component
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

        if (availableExecutableItems.isNotEmpty()) {
            availableExecutableItems.forEach { executableCombo.addItem(it) }
            executableCombo.selectedIndex = 0
            val variants = availableExecutableItems.first().exec.variants
            if (variants.isNotEmpty()) {
                variants.forEach { variantCombo.addItem(BinaryExecutableVariantItem(it)) }
                variantCombo.selectedIndex = 0
            }
        }

        executableCombo.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                variantCombo.removeAllItems()
                (event.item as BinaryExecutableItem).exec.variants
                    .forEach { variantCombo.addItem(BinaryExecutableVariantItem(it)) }
            }
        }

        panel.add(executableLabel, gridBag.nextLine().next())
        panel.add(executableCombo, gridBag.next().coverLine())
        executableLabel.labelFor = executableCombo

        panel.add(variantLabel, gridBag.nextLine().next())
        panel.add(variantCombo, gridBag.next().coverLine())
        variantLabel.labelFor = variantCombo

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
        runConfiguration.apply {
            executable = (executableCombo.selectedItem as? BinaryExecutableItem)?.exec
            variant = (variantCombo.selectedItem as? BinaryExecutableVariantItem)?.variant
        }
        commonProgramParameters.applyTo(runConfiguration)
    }

    override fun resetEditorFrom(runConfiguration: BinaryRunConfiguration) {
        commonProgramParameters.reset(runConfiguration)

        val executable = runConfiguration.executable
        val variant = runConfiguration.variant
        if (executable != null) {
            val item = BinaryExecutableItem(executable)
            if (availableExecutableItems.contains(item)) {
                executableCombo.selectedItem = item

                if (variant != null && executable.variants.contains(variant)) {
                    variantCombo.selectedItem = BinaryExecutableVariantItem(variant)
                } else {
                    variantCombo.selectedIndex = -1
                }
            } else {
                executableCombo.selectedIndex = -1
                variantCombo.removeAllItems()
            }
        } else {
            executableCombo.selectedIndex = -1
            variantCombo.removeAllItems()
        }
    }

    private data class BinaryExecutableItem(
        val exec: BinaryExecutable
    ) {
        override fun toString() = exec.projectPrefix + exec.name
    }

    private data class BinaryExecutableVariantItem(
        val variant: BinaryExecutable.Variant
    ) {
        override fun toString() = variant.name
    }
}