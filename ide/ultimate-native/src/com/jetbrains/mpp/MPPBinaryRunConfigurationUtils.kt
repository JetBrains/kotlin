/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
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

class MPPBinaryRunConfigurationType : ConfigurationTypeBase(
    KonanBundle.message("id.runConfiguration"),
    KonanBundle.message("label.applicationName.text"),
    KonanBundle.message("label.applicationDescription.text"),
    AllIcons.RunConfigurations.Application
) {
    val factory: ConfigurationFactory
        get() = object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return MPPBinaryRunConfiguration(project, this, null)
            }

            override fun getId(): String = KonanBundle.message("id.factory")
        }

    init {
        addFactory(factory)
    }

    fun createEditor(project: Project): SettingsEditor<out MPPBinaryRunConfiguration> {
        return MPPBinaryRunConfigurationSettingsEditor(project)
    }

    companion object {
        val instance: MPPBinaryRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(MPPBinaryRunConfigurationType::class.java)
    }
}


class MPPBinaryRunConfigurationSettingsEditor(val project: Project) :
    BinaryRunConfigurationSettingsEditorBase<MPPBinaryRunConfiguration>() {

    override fun getWorkspace() = MPPWorkspace.getInstance(project)
}