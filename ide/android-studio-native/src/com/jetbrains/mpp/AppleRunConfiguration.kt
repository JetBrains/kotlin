/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.konan.MPPWorkspace
import com.jetbrains.mpp.execution.AppleDevice
import com.jetbrains.mpp.execution.AppleSimulator
import com.jetbrains.mpp.execution.Device
import org.jdom.Element
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File

class AppleRunConfiguration(project: Project, configurationFactory: MobileConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, configurationFactory, name) {

    var xcodeproj: String? = MPPWorkspace.getInstance(project).xcproject

    val xcodeScheme: String = "iosApp" // TODO: Use provided.

    val xcodeSdk: String = "iphonesimulator" // TODO: Use provided.

    val iosBuildDirectory = "ios_build" // TODO: Allow configuration.

    val workingDirectory = project.basePath?.let { File(it) }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MobileRunConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        (environment.executionTarget as? Device)?.createState(this, environment)

    override fun getBeforeRunTasks(): MutableList<BeforeRunTask<*>> {
        val result = mutableListOf<BeforeRunTask<*>>()
        xcodeproj?.let { result.add(BuildIOSAppTask()) }
        return result
    }

    override fun canRunOn(target: ExecutionTarget): Boolean = target is Device

    fun getProductBundle(environment: ExecutionEnvironment): File {
        return workingDirectory!!.resolve(iosBuildDirectory).resolve("Debug-iphonesimulator/$xcodeScheme.app")
    }
}
