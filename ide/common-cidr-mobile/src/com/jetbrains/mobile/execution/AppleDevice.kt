package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceUtil
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue
import java.io.File

sealed class AppleDevice(id: String, name: String, osVersion: String) : Device(id, name, "iOS", osVersion) {
    abstract fun install(bundle: File, project: Project): GeneralCommandLine

    abstract val arch: ArchitectureValue

    abstract val platformType: ApplePlatform.Type
}

class ApplePhysicalDevice(val raw: AMDevice) : AppleDevice(
    raw.deviceIdentifier,
    raw.name,
    raw.productVersion ?: "Unknown"
) {
    override fun install(bundle: File, project: Project): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        AMDeviceUtil.installApplicationInBackgroundAndAcquireDebugInfo(raw, true, bundle, project, commandLine)
        commandLine.exePath += "/" + FileUtil.getNameWithoutExtension(bundle)
        return commandLine
    }

    override val arch: ArchitectureValue
        get() = ArchitectureValue(raw.cpuArchitecture!!)

    override val platformType: ApplePlatform.Type
        get() = ApplePlatform.Type.IOS
}

class AppleSimulator(val raw: SimulatorConfiguration) : AppleDevice(
    raw.udid,
    raw.name,
    raw.version
) {
    override fun install(bundle: File, project: Project): GeneralCommandLine =
        GeneralCommandLine(bundle.absolutePath)

    override val arch: ArchitectureValue
        get() = raw.launchArchitecture

    override val platformType = when (raw.launchDeviceFamily) {
        SimulatorConfiguration.IPHONE_FAMILY -> ApplePlatform.Type.IOS_SIMULATOR
        SimulatorConfiguration.IPAD_FAMILY -> ApplePlatform.Type.IOS_SIMULATOR
        SimulatorConfiguration.TV_FAMILY -> ApplePlatform.Type.TVOS_SIMULATOR
        SimulatorConfiguration.WATCH_FAMILY -> ApplePlatform.Type.WATCH_SIMULATOR
        else -> error("Unknown SimulatorConfiguration.launchDeviceFamily=${raw.launchDeviceFamily}")
    }
}