/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.testing.CidrLauncher

abstract class AppleDevice(id: String, name: String, osVersion: String) : Device(id, name, "iOS", osVersion) {
    override fun createState(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrCommandLineState =
        CidrCommandLineState(environment, createLauncher(configuration))

    protected abstract fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher
}

class ApplePhysicalDevice(private val raw: AMDevice) : AppleDevice(
    raw.deviceIdentifier,
    raw.name,
    raw.productVersion ?: "Unknown"
) {
    override fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher =
        AppleLauncher(configuration, ArchitectureType.forArchitecture(raw.cpuArchitecture), raw)
}

class AppleSimulator(private val wrapped: SimulatorConfiguration) : AppleDevice(
    wrapped.udid,
    wrapped.name,
    wrapped.version
) {
    override fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher =
        AppleLauncher(configuration, wrapped.launchArchitecture.type, null)
}