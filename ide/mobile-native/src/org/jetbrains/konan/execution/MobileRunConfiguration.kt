/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationModule
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.execution.CidrExecutableDataHolder
import com.jetbrains.cidr.execution.CidrRunConfiguration
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jdom.Element
import org.jetbrains.konan.isAndroid
import org.jetbrains.konan.isApple
import java.io.File

abstract class MobileRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    CidrRunConfiguration<MobileBuildConfiguration, MobileBuildTarget>(project, factory, name),
    CidrExecutableDataHolder {

    private var _module = RunConfigurationModule(project).also { it.setModuleToAnyFirstIfNotSpecified() }
    var module: Module
        get() = _module.module!!
        set(value) {
            _module.module = value
        }

    override fun canRunOn(target: ExecutionTarget): Boolean =
        target is Device &&
                (canRunOnApple && target is AppleDevice) ||
                (canRunOnAndroid && target is AndroidDevice)

    val canRunOnAndroid: Boolean get() = module.isAndroid
    val canRunOnApple: Boolean get() = module.isApple

    open fun getProductBundle(environment: ExecutionEnvironment): File {
        val moduleRoot = ExternalSystemApiUtil.getExternalProjectPath(module)?.let { File(it) }
            ?: throw IllegalStateException()
        return when (val device = environment.executionTarget) {
            is AndroidDevice -> {
                File(moduleRoot, FileUtil.join("build", "outputs", "apk", "debug", "${moduleRoot.name}-debug.apk"))
            }
            is AppleDevice -> {
                @Suppress("SpellCheckingInspection")
                val deviceType = when (device) {
                    is ApplePhysicalDevice -> "iphoneos"
                    is AppleSimulator -> "iphonesimulator"
                    else -> throw IllegalStateException()
                }
                File(moduleRoot, FileUtil.join("build", "bin", "iosAppMain", "Debug-$deviceType", "iosApp.app"))
            }
            else -> throw IllegalStateException()
        }
    }

    private val helper = MobileBuildConfigurationHelper(project)
    override fun getHelper(): MobileBuildConfigurationHelper = helper

    override fun getResolveConfiguration(target: ExecutionTarget): OCResolveConfiguration? = null

    private var _executableData: ExecutableData? = null

    override fun getExecutableData() = _executableData
    override fun setExecutableData(executableData: ExecutableData?) {
        _executableData = executableData
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        _module.writeExternal(element)
        _executableData?.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        _module.readExternal(element)
        _executableData = ExecutableData.loadExternal(element)
    }
}

class MobileAppRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfiguration(project, factory, name) {

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileRunConfigurationEditor(project, helper)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState? =
        (environment.executionTarget as? Device)?.createState(this, environment)
}