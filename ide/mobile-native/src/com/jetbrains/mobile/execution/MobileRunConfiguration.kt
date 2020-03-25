/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

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
import com.jetbrains.cidr.execution.CidrRunConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.mobile.isAndroid
import com.jetbrains.mobile.isApple
import com.jetbrains.mobile.isMobileAppMain
import org.jdom.Element
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File

abstract class MobileRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    CidrRunConfiguration<MobileBuildConfiguration, MobileBuildTarget>(project, factory, name) {

    private var _module = RunConfigurationModule(project).also {
        it.module = project.allModules().firstOrNull { module -> isSuitable(module) }
    }
    var module: Module?
        get() = _module.module
        set(value) {
            _module.module = value
        }

    protected abstract fun isSuitable(module: Module): Boolean

    override fun canRunOn(target: ExecutionTarget): Boolean =
        target is Device &&
                (module?.isApple == true && target is AppleDevice) ||
                (module?.isAndroid == true && target is AndroidDevice)

    open fun getProductBundle(environment: ExecutionEnvironment): File {
        val moduleRoot = ExternalSystemApiUtil.getExternalProjectPath(module!!)?.let { File(it) }
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
                File(moduleRoot, FileUtil.join("build", "bin", "iosAppMain", "Debug-$deviceType", "${moduleRoot.name}.app"))
            }
            else -> throw IllegalStateException()
        }
    }

    private val helper = MobileBuildConfigurationHelper(project)
    override fun getHelper(): MobileBuildConfigurationHelper = helper

    override fun getResolveConfiguration(target: ExecutionTarget): OCResolveConfiguration? = null

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        _module.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        _module.readExternal(element)
    }

    override fun clone(): MobileRunConfiguration {
        val result = super.clone() as MobileRunConfiguration
        result._module = RunConfigurationModule(project).also { it.module = this.module }
        return result
    }

    override fun checkConfiguration() {
        super.checkConfiguration()
        _module.checkForWarning()
    }

    companion object
}

class MobileAppRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfiguration(project, factory, name) {

    override fun isSuitable(module: Module): Boolean = module.isMobileAppMain

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileRunConfigurationEditor(project, helper, ::isSuitable)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState? =
        (environment.executionTarget as? Device)?.createState(this, environment)
}