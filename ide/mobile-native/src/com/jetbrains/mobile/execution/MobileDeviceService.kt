package com.jetbrains.mobile.execution

import com.android.ddmlib.AndroidDebugBridge
import com.android.sdklib.AndroidVersion
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.AtomicClearableLazyValue
import com.jetbrains.mobile.AndroidToolkit

class MobileDeviceService(project: Project) : DeviceService(project) {
    override fun getAll(): List<Device> = getApplePhysicalDevices() + getAndroidDevices() + getAppleSimulators()

    fun getAndroidDevices(): List<AndroidDevice> =
        getAndroidPhysicalDevices() + androidEmulators.value

    private fun getAndroidPhysicalDevices(): List<AndroidPhysicalDevice> =
        adb?.devices
            ?.filter { !it.isEmulator }
            ?.map(::AndroidPhysicalDevice)
            ?: emptyList()

    private var androidEmulators = AtomicClearableLazyValue.create {
        AndroidToolkit.getInstance(project).avdData.listFiles { file -> file.extension == "ini" }
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
                val toolkit = AndroidToolkit.getInstance(project)
                _adb = toolkit.adb?.let { adbBinary ->
                    val forceNew = _adb != null
                    log.info("Creating ADB bridge for ${toolkit.adb}, forceNew = $forceNew")
                    AndroidDebugBridge.createBridge(adbBinary.path, forceNew)
                }
            }
            return _adb
        }

    companion object {
        fun getInstance(project: Project): MobileDeviceService =
            DeviceService.getInstance(project) as MobileDeviceService

        private val log = logger<MobileDeviceService>()
    }
}