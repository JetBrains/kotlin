/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceUtil
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.File

abstract class AppleDevice(id: String, name: String, osVersion: String) : Device(id, name, "iOS", osVersion) {
    override fun createState(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrCommandLineState =
        CidrCommandLineState(environment, createLauncher(configuration))

    protected abstract fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher

    abstract fun install(bundle: File, project: Project): GeneralCommandLine
}

class ApplePhysicalDevice(private val raw: AMDevice) : AppleDevice(
    raw.deviceIdentifier,
    raw.name,
    raw.productVersion ?: "Unknown"
) {
    override fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher =
        ApplePhysicalDeviceLauncher(configuration, ArchitectureType.forArchitecture(raw.cpuArchitecture), this, raw)

    override fun install(bundle: File, project: Project): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        AMDeviceUtil.installApplicationInBackgroundAndAcquireDebugInfo(raw, true, bundle, project, commandLine)
        commandLine.exePath += "/" + FileUtil.getNameWithoutExtension(bundle)
        return commandLine
    }
}

class AppleSimulator(private val raw: SimulatorConfiguration) : AppleDevice(
    raw.udid,
    raw.name,
    raw.version
) {
    override fun createLauncher(configuration: MobileRunConfiguration): CidrLauncher =
        AppleSimulatorLauncher(configuration, raw.launchArchitecture.type, this)

    override fun install(bundle: File, project: Project): GeneralCommandLine =
        GeneralCommandLine(bundle.absolutePath)
}