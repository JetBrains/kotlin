/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrExecutableDataHolder
import com.jetbrains.cidr.execution.CidrRunConfiguration
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jdom.Element
import java.io.File

class MobileRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    CidrRunConfiguration<MobileBuildConfiguration, MobileBuildTarget>(project, factory, name),
    CidrExecutableDataHolder {

    private val helper = MobileBuildConfigurationHelper(project)
    override fun getHelper(): MobileBuildConfigurationHelper = helper

    override fun getResolveConfiguration(target: ExecutionTarget): OCResolveConfiguration? = null

    override fun getConfigurationEditor(): SettingsEditor<MobileRunConfiguration> =
        MobileRunConfigurationEditor(project, helper)

    override fun canRunOn(target: ExecutionTarget): Boolean =
        target is Device &&
                (canRunOnApple && target is AppleDevice) ||
                (canRunOnAndroid && target is AndroidDevice)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CidrCommandLineState? =
        (environment.executionTarget as? Device)?.createState(this, environment)

    fun getProductBundle(environment: ExecutionEnvironment): File {
        // TODO decide if we want to allow custom executable selection
        //  and retrieve info from gradle when executable not selected explicitly
        return File(_executableData!!.path!!)
    }

    val canRunOnAndroid: Boolean get() = _executableData!!.path!!.endsWith(".apk")
    val canRunOnApple: Boolean get() = _executableData!!.path!!.endsWith(".app")

    private var _executableData: ExecutableData? = null

    override fun getExecutableData() = _executableData
    override fun setExecutableData(executableData: ExecutableData?) {
        _executableData = executableData
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        if (_executableData != null) _executableData!!.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        _executableData = ExecutableData.loadExternal(element)
    }
}

