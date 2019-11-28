/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.android.ddmlib.AndroidDebugBridge
import com.android.sdklib.AndroidVersion
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceManager
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorsRegistry
import com.jetbrains.mobile.AndroidToolkit

class DeviceService {
    fun getAll(): List<Device> = getAppleDevices() + getAndroidDevices() + androidEmulators.value + getAppleSimulators()

    private fun getAppleDevices(): List<ApplePhysicalDevice> =
        AMDeviceManager.getInstance().devices
            .filter { it.deviceType.isIOS }
            .map(::ApplePhysicalDevice)

    private fun getAppleSimulators(): List<AppleSimulator> =
        SimulatorsRegistry.getInstance().configurations.map(::AppleSimulator)

    private fun getAndroidDevices(): List<AndroidPhysicalDevice> =
        adb?.devices
            ?.filter { !it.isEmulator }
            ?.map(::AndroidPhysicalDevice)
            ?: emptyList()

    private var androidEmulators = AtomicClearableLazyValue.create {
        AndroidToolkit.avdData.listFiles { file -> file.extension == "ini" }
            ?.map { file ->
                val prefix = "target=android-"
                val version = file.readLines()
                    .find { it.startsWith(prefix) }
                    ?.removePrefix(prefix)
                    ?.let { AndroidVersion(it) }
                AndroidEmulator(file.nameWithoutExtension, version)
            }
            ?: emptyList()
    }

    private var _adb: AndroidDebugBridge? = null

    val adb: AndroidDebugBridge?
        @Synchronized
        get() {
            val disconnected = _adb?.isConnected != true
            if (disconnected) {
                AndroidDebugBridge.initIfNeeded(true)
                _adb = AndroidToolkit.adb?.let { adbBinary ->
                    val forceNew = _adb != null
                    log.info("Creating ADB bridge for ${AndroidToolkit.adb}, forceNew = $forceNew")
                    AndroidDebugBridge.createBridge(adbBinary.path, forceNew)
                }
            }
            return _adb
        }

    companion object {
        val instance: DeviceService get() = service()
        private val log = logger<DeviceService>()
    }
}