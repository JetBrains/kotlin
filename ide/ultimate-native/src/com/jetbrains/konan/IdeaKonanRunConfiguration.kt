/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionException
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.konan.debugger.KonanLLDBDriverConfiguration
import org.jdom.Element


class IdeaKonanRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    executable: KonanExecutable?
) : BinaryRunConfigurationBase(project, factory, executable) {

    override fun lldbDriverConfiguration(env: ExecutionEnvironment): LLDBDriverConfiguration {
        val lldbHome = IdeaKonanWorkspace.getInstance(env.project).lldbHome
            ?: throw ExecutionException("Debug is impossible without lldb binaries required by Kotlin/Native")

        return KonanLLDBDriverConfiguration(lldbHome)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return IdeaKonanRunConfigurationSettingsEditor(project)
    }

    override fun getWorkspace(): WorkspaceBase = IdeaKonanWorkspace.getInstance(project)
}