/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.EmulatorConsole
import com.android.ddmlib.IDevice
import com.android.sdklib.AndroidVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.intellij.openapi.util.Key
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceManager
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorsRegistry
import org.jetbrains.konan.AndroidToolkit
import java.util.concurrent.CompletableFuture

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

    internal fun launchAndroidEmulator(emulator: AndroidEmulator): IDevice {
        // If already running, return immediately
        adb?.devices?.find { it.avdName == emulator.id }?.let { return it }

        val commandLine = GeneralCommandLine(AndroidToolkit.emulator!!.path, "-avd", emulator.id)
        val handler = OSProcessHandler(commandLine)
        handler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                log.info("avd: " + event.text)
            }
        })

        val future = CompletableFuture<IDevice>()
        val deviceListener = object : AndroidDebugBridge.IDeviceChangeListener {
            override fun deviceChanged(device: IDevice, changeMask: Int) {
                // device might not have `avdName` set yet, so we ask for it explicitly
                val avdName = EmulatorConsole.getConsole(device).avdName
                if (avdName == emulator.id) {
                    log.debug("Device appeared: $avdName")
                    future.complete(device)
                }
            }

            override fun deviceConnected(device: IDevice) {}
            override fun deviceDisconnected(device: IDevice) {}
        }

        AndroidDebugBridge.addDeviceChangeListener(deviceListener)
        try {
            return future.get()
        } finally {
            AndroidDebugBridge.removeDeviceChangeListener(deviceListener)
        }
    }

    private val adb by lazy {
        AndroidToolkit.adb?.let { adbBinary ->
            AndroidDebugBridge.init(true)
            AndroidDebugBridge.createBridge(adbBinary.absolutePath, false)
        }
    }

    companion object {
        val instance: DeviceService get() = service()
        private val log = logger<DeviceService>()
    }
}