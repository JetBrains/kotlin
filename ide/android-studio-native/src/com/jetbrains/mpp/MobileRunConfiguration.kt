/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.konan.MPPWorkspace
import org.jdom.Element

class MobileRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name) {

    var xcodeproj: String? = MPPWorkspace.getInstance(project).xcproject

    val xcodeScheme: String = "iosApp" // TODO: Use provided.

    val xcodeSdk: String = "iphonesimulator" // TODO: Use provided.

    val iosBuildDirectory = "./ios_build" // TODO: Allow configuration.

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MobileRunConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return MobileCommandLineState(environment, xcodeproj)
    }

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
        val result = mutableListOf<BeforeRunTask<*>>()
        xcodeproj?.let { result.add(BuildIOSAppTask()) }
        return result
    }
}
