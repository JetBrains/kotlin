package com.jetbrains.mobile.execution.testing

import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrCommandLineConfigurator
import com.jetbrains.cidr.execution.CidrProgramParameters
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorRuntime
import com.jetbrains.cidr.execution.testing.OCTestCommandLineConfigurator
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue
import com.jetbrains.mobile.execution.*
import java.io.File
import java.util.*

class AppleXCTestLauncher(configuration: MobileTestRunConfiguration, environment: ExecutionEnvironment, device: AppleDevice) :
    AppleLauncher<MobileTestRunConfiguration>(configuration, environment, device) {

    override fun createInstaller(bundle: File) = object : AppleInstaller(configuration, device, environment, bundle) {
        override fun createCommandLineBuilder(
            params: CidrProgramParameters,
            platform: ApplePlatform,
            arch: ArchitectureValue
        ): CidrCommandLineConfigurator {
            val testBundle = configuration.getTestRunnerBundle(device)
            val testScope = (myEnvironment.state as AppleXCTestCommandLineState).testScope()
            val productFileName = bundle.nameWithoutExtension
            val sessionID = UUID.randomUUID()
            val appleDevice = device

            return object : OCTestCommandLineConfigurator(
                this, params, platform, device.arch, null, productFileName,
                File(bundle, productFileName), testBundle.path, false, testScope, sessionID
            ) {
                override fun getDevice(): AMDevice = (appleDevice as ApplePhysicalDevice).raw
                override fun getSimulator(): SimulatorRuntime = (appleDevice as AppleSimulator).raw.runtime

                override fun getProductModuleName(): String? = testBundle.nameWithoutExtension
            }
        }
    }
}