/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.kmm.KmmBundle
import com.jetbrains.mobile.execution.AppleDevice
import com.jetbrains.mobile.execution.DeviceService
import org.jetbrains.kotlin.idea.facet.selectedItemTyped
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class AppleRunConfigurationEditor(
    private val project: Project
) : SettingsEditor<AppleRunConfiguration>() {
    private val schemeCombo = ComboBox<String>()
    private val targetCombo = ComboBox<AppleDeviceItem>()

    private val gridBag = GridBag()
        .setDefaultFill(GridBagConstraints.BOTH)
        .setDefaultAnchor(GridBagConstraints.CENTER)
        .setDefaultWeightX(1, 1.0)
        .setDefaultInsets(0, JBUI.insets(0, 0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP))
        .setDefaultInsets(1, JBUI.insetsBottom(UIUtil.DEFAULT_VGAP))

    override fun resetEditorFrom(configuration: AppleRunConfiguration) {
        with(schemeCombo) {
            removeAllItems()
            configuration.xcProjectFile?.schemes?.forEach { addItem(it) }
            selectedItem = configuration.xcodeScheme
        }

        with(targetCombo) {
            removeAllItems()
            DeviceService.getInstance(project).getIosDevices()
                .map { AppleDeviceItem(it) }
                .forEach { addItem(it) }
            selectedItem = AppleDeviceItem(configuration.executionTarget)
        }
    }

    override fun applyEditorTo(configuration: AppleRunConfiguration) {
        configuration.xcodeScheme = schemeCombo.selectedItemTyped
        targetCombo.selectedItemTyped?.device?.let { configuration.executionTarget = it }
    }

    override fun createEditor() = JPanel(GridBagLayout()).apply {
        val schemeLabel = JBLabel(KmmBundle.message("apple.runconfig.xcodeSchemeLabel"))
        schemeLabel.labelFor = schemeCombo

        add(schemeLabel, gridBag.nextLine().next())
        add(schemeCombo, gridBag.next().coverLine())

        val targetLabel = JBLabel(KmmBundle.message("apple.runconfig.execTargetLabel"))
        targetLabel.labelFor = targetCombo

        add(targetLabel, gridBag.nextLine().next())
        add(targetCombo, gridBag.next().coverLine())
    }
}

private data class AppleDeviceItem(
    val device: AppleDevice
) {
    override fun toString() = device.displayName
}
