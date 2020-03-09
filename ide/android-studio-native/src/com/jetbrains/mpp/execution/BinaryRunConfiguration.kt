/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.execution

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.konan.*
import com.jetbrains.mpp.AppleLLDBDriverConfiguration
import com.jetbrains.mpp.ProjectWorkspace
import org.jdom.Element
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import javax.swing.JComponent

class BinaryRunConfigurationSettingsEditor(val project: Project) : SettingsEditor<BinaryRunConfiguration>()
{
    override fun resetEditorFrom(s: BinaryRunConfiguration) {
        TODO("Not yet implemented")
    }

    override fun createEditor(): JComponent {
        TODO("Not yet implemented")
    }

    override fun applyEditorTo(s: BinaryRunConfiguration) {
        TODO("Not yet implemented")
    }
}

class BinaryRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    executable: KonanExecutable?
) : BinaryRunConfigurationBase(project, factory, executable) {

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return BinaryRunConfigurationSettingsEditor(project)
    }

    override fun lldbDriverConfiguration(env: ExecutionEnvironment) = AppleLLDBDriverConfiguration()

    override fun getWorkspace(): WorkspaceBase = ProjectWorkspace.getInstance(project)
}

class BinaryRunConfigurationType : ConfigurationTypeBase(
    KonanBundle.message("id.runConfiguration"),
    KonanBundle.message("label.applicationName.text"),
    KonanBundle.message("label.applicationDescription.text"),
    AllIcons.RunConfigurations.Application
) {
    val factory: ConfigurationFactory
        get() = object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return BinaryRunConfiguration(project, this, null)
            }

            override fun getId(): String = KonanBundle.message("id.factory")
        }

    init {
        addFactory(factory)
    }

    fun createEditor(project: Project): SettingsEditor<out BinaryRunConfiguration> {
        return BinaryRunConfigurationSettingsEditor(project)
    }

    companion object {
        val instance: BinaryRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(BinaryRunConfigurationType::class.java)
    }
}