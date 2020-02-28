/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.OCCommandLineConfigurator
import com.jetbrains.cidr.execution.OCInstaller
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.mpp.AppleRunConfiguration
import java.io.File

abstract class AppleInstaller(
    private val configuration: AppleRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File
) : OCInstaller(environment, appBundle, appBundle, false) {

    override fun doInstall(): GeneralCommandLine {
        val device = myEnvironment.executionTarget as AppleDevice
        val commandLine = device.createCommandLine(bundle, project)

        val params = SimpleProgramParameters().also {
            it.workingDirectory = File(commandLine.exePath).parentFile.parent
            it.isPassParentEnvs = false
        }
        val platform = AppleSdkManager.getInstance().findPlatformByType(
            if (device is AppleSimulator) ApplePlatform.Type.IOS_SIMULATOR
            else ApplePlatform.Type.IOS
        )!!

        val configurator = OCCommandLineConfigurator(project, params, platform, device.arch, null, false)
        configurator.configureCommandLine(commandLine)

        return commandLine
    }

    override fun getRunConfiguration(): AppleRunConfiguration = configuration

    override fun getAppWorkingDir(): File? = null
}

class ApplePhysicalDeviceInstaller(
    configuration: AppleRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File,
    raw: AMDevice
) : AppleInstaller(configuration, environment, appBundle) {
    val rawDevice: AMDevice = raw
}

class AppleSimulatorInstaller(
    configuration: AppleRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File,
    raw: SimulatorConfiguration
) : AppleInstaller(configuration, environment, appBundle) {
}