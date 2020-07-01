package com.jetbrains.mobile.execution

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceManager
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorsRegistry

open class DeviceService(protected val project: Project) {
    open fun getAll(): List<Device> = getAppleDevices() + getAppleSimulators()

    protected fun getAppleDevices(): List<ApplePhysicalDevice> =
        AMDeviceManager.getInstance().devices
            .filter { it.deviceType.isIOS }
            .map(::ApplePhysicalDevice)

    protected fun getAppleSimulators(): List<AppleSimulator> =
        SimulatorsRegistry.getInstance().configurations.map(::AppleSimulator)

    companion object {
        fun getInstance(project: Project): DeviceService = project.service()

        @JvmStatic
        protected val log = logger<DeviceService>()
    }
}