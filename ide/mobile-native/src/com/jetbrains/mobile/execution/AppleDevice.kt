/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceUtil
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.testing.CidrTestScope
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue
import com.jetbrains.mobile.execution.testing.AppleXCTestCommandLineState
import com.jetbrains.mobile.execution.testing.MobileTestRunConfiguration
import java.io.File
import java.nio.file.Paths

abstract class AppleDevice(id: String, name: String, osVersion: String) : Device(id, name, "iOS", osVersion) {
    override fun createState(configuration: MobileAppRunConfiguration, environment: ExecutionEnvironment): CidrCommandLineState =
        CidrCommandLineState(environment, createLauncher(configuration, environment)).also {
            it.consoleBuilder = CidrConsoleBuilder(configuration.project, null, configuration.project.basePath?.let { Paths.get(it) })
        }

    override fun createState(configuration: MobileTestRunConfiguration, environment: ExecutionEnvironment): CommandLineState =
        createState(configuration, environment, null)

    fun createState(
        configuration: MobileTestRunConfiguration,
        environment: ExecutionEnvironment,
        testScope: CidrTestScope?
    ): AppleXCTestCommandLineState =
        AppleXCTestCommandLineState(configuration, createLauncher(configuration, environment), environment, environment.executor, testScope)

    protected abstract fun createLauncher(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrLauncher

    abstract fun install(bundle: File, project: Project): GeneralCommandLine

    abstract val arch: ArchitectureValue
}

class ApplePhysicalDevice(private val raw: AMDevice) : AppleDevice(
    raw.deviceIdentifier,
    raw.name,
    raw.productVersion ?: "Unknown"
) {
    override fun createLauncher(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrLauncher =
        ApplePhysicalDeviceLauncher(configuration, environment, this, raw)

    override fun install(bundle: File, project: Project): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        AMDeviceUtil.installApplicationInBackgroundAndAcquireDebugInfo(raw, true, bundle, project, commandLine)
        commandLine.exePath += "/" + FileUtil.getNameWithoutExtension(bundle)
        return commandLine
    }

    override val arch: ArchitectureValue
        get() = ArchitectureValue(raw.cpuArchitecture!!)
}

class AppleSimulator(private val raw: SimulatorConfiguration) : AppleDevice(
    raw.udid,
    raw.name,
    raw.version
) {
    override fun createLauncher(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrLauncher =
        AppleSimulatorLauncher(configuration, environment, this, raw)

    override fun install(bundle: File, project: Project): GeneralCommandLine =
        GeneralCommandLine(bundle.absolutePath)

    override val arch: ArchitectureValue
        get() = raw.launchArchitecture
}