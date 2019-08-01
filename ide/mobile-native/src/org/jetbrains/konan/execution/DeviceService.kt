/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.AndroidDebugBridge
import com.intellij.openapi.components.service
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceManager
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorsRegistry
import org.jetbrains.konan.AndroidToolkit

class DeviceService {
    fun getAll(): List<Device> = getAppleDevices() + getAndroidDevices() + getAppleSimulators()

    private fun getAppleDevices(): List<ApplePhysicalDevice> =
        AMDeviceManager.getInstance().devices
            .filter { it.deviceType.isIOS }
            .map(::ApplePhysicalDevice)

    private fun getAppleSimulators(): List<AppleSimulator> =
        SimulatorsRegistry.getInstance().configurations.map(::AppleSimulator)

    private fun getAndroidDevices(): List<AndroidDevice> =
        adb.devices.map(::AndroidDevice)

    private val adb by lazy {
        AndroidDebugBridge.init(true)
        AndroidDebugBridge.createBridge(AndroidToolkit.adb!!.absolutePath, false)
    }

    companion object {
        val instance: DeviceService get() = service()
    }
}