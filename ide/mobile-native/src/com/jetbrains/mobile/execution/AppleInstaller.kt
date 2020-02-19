/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrProgramParameters
import com.jetbrains.cidr.execution.OCCommandLineConfigurator
import com.jetbrains.cidr.execution.OCInstaller
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorRuntime
import com.jetbrains.cidr.execution.testing.OCTestCommandLineConfigurator
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.mobile.execution.testing.AppleXCTestCommandLineState
import com.jetbrains.mobile.execution.testing.MobileTestRunConfiguration
import java.io.File
import java.util.*

abstract class AppleInstaller(
    private val configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File
) : OCInstaller(environment, appBundle, appBundle, false) {

    override fun doInstall(): GeneralCommandLine {
        val device = myEnvironment.executionTarget as AppleDevice
        val commandLine = device.install(bundle, project)

        val params = CidrProgramParameters().also {
            it.workingDirectory = File(commandLine.exePath).parentFile.parent
            it.isPassParentEnvs = false
        }
        val platform = AppleSdkManager.getInstance().findPlatformByType(platformType)!!

        val configurator =
            if (configuration is MobileTestRunConfiguration) {
                val testBundle = configuration.getTestRunnerBundle(myEnvironment)
                val testScope = (myEnvironment.state as AppleXCTestCommandLineState).testScope()
                val productFileName = bundle.nameWithoutExtension
                val sessionID = UUID.randomUUID()

                object : OCTestCommandLineConfigurator(
                    this, params, platform, device.arch, null, productFileName,
                    File(bundle, productFileName), testBundle.path, false, testScope, sessionID
                ) {
                    override fun getDevice(): AMDevice = rawDevice
                    override fun getSimulator(): SimulatorRuntime = rawSimulator

                    override fun getProductModuleName(): String? = null // TODO
                }
            } else {
                OCCommandLineConfigurator(project, params, platform, device.arch, null, false)
            }
        configurator.configureCommandLine(commandLine)

        return commandLine
    }

    protected abstract val platformType: ApplePlatform.Type
    protected open val rawDevice: AMDevice get() = throw IllegalStateException()
    protected open val rawSimulator: SimulatorRuntime get() = throw IllegalStateException()

    override fun getRunConfiguration(): MobileRunConfiguration = configuration
}

class ApplePhysicalDeviceInstaller(
    configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File,
    raw: AMDevice
) : AppleInstaller(configuration, environment, appBundle) {
    override val platformType: ApplePlatform.Type = ApplePlatform.Type.IOS
    override val rawDevice: AMDevice = raw
}

class AppleSimulatorInstaller(
    configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment,
    appBundle: File,
    raw: SimulatorConfiguration
) : AppleInstaller(configuration, environment, appBundle) {
    override val platformType: ApplePlatform.Type = ApplePlatform.Type.IOS_SIMULATOR
    override val rawSimulator: SimulatorRuntime = raw.runtime
}