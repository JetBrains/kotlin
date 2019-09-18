/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.EmulatorConsole
import com.android.ddmlib.IDevice
import com.android.sdklib.AndroidVersion
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.konan.AndroidToolkit
import org.jetbrains.konan.MobileBundle
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AndroidDevice(uniqueID: String, name: String, osVersion: AndroidVersion?) : Device(
    uniqueID,
    name,
    "Android",
    osVersion?.apiString ?: "unknown"
) {
    override fun createState(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): AndroidCommandLineState =
        AndroidCommandLineState(configuration, environment)

    fun installAndLaunch(apk: File, project: Project, waitForDebugger: Boolean = false): AndroidProcessHandler {
        val handler = AndroidProcessHandler()
        runBackgroundableTask(MobileBundle.message("run.waiting"), project, cancellable = false) { indicator ->
            try {
                val raw = prepareDevice()

                indicator.isIndeterminate = false
                indicator.fraction = 0.1
                indicator.text = MobileBundle.message("run.preparing")

                val (appId, activity) = getAppMetadata(apk)
                handler.appId = appId
                handler.raw = raw

                indicator.fraction = 0.3
                indicator.text = MobileBundle.message("run.installing")

                raw.installPackage(apk.absolutePath, true)
                handler.prepareForLaunch()

                indicator.fraction = 0.8
                indicator.text = MobileBundle.message("run.starting")

                val receiver = CollectingOutputReceiver()
                val options = if (waitForDebugger) "-D" else ""
                raw.executeShellCommand(
                    "am start $options -n \"$appId/$activity\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", receiver
                )
                log.info("Launched app with output: ${receiver.output.trimEnd()}")
            } catch (e: Throwable) {
                handler.destroyProcess()
                if (e is ExecutionException) {
                    val message = e.message ?: e.toString()
                    val window = if (waitForDebugger) ToolWindowId.DEBUG else ToolWindowId.RUN
                    runInEdt {
                        ToolWindowManager.getInstance(project).notifyByBalloon(window, MessageType.ERROR, message)
                    }
                    handler.notifyTextAvailable("$message\n", ProcessOutputType.SYSTEM)
                } else {
                    throw e
                }
            }
        }
        return handler
    }

    protected abstract fun prepareDevice(): IDevice
}

class AndroidPhysicalDevice(private val raw: IDevice) : AndroidDevice(
    raw.serialNumber,
    raw.getProperty(IDevice.PROP_DEVICE_MODEL) ?: "Unknown Device",
    raw.version
) {
    override fun prepareDevice(): IDevice = raw
}

class AndroidEmulator(avdName: String, osVersion: AndroidVersion?) : AndroidDevice(
    avdName,
    avdName.replace('_', ' '),
    osVersion
) {
    override fun prepareDevice(): IDevice {
        // If already running, return immediately
        DeviceService.instance.adb?.devices?.find { it.avdName == id }?.let { return it }

        val commandLine = GeneralCommandLine(AndroidToolkit.emulator!!.path, "-avd", id)
        val avdHandler = OSProcessHandler(commandLine)
        avdHandler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                log.info("avd: " + event.text)
            }
        })

        val future = CompletableFuture<IDevice>()
        val deviceListener = object : AndroidDebugBridge.IDeviceChangeListener {
            override fun deviceChanged(device: IDevice, changeMask: Int) {
                // device might not have `avdName` set yet, so we ask for it explicitly
                val avdName = device.avdName ?: EmulatorConsole.getConsole(device).avdName
                if (avdName == id) {
                    log.info("Device appeared: $avdName")
                    future.complete(device)
                }
            }

            override fun deviceConnected(device: IDevice) {}
            override fun deviceDisconnected(device: IDevice) {}
        }

        AndroidDebugBridge.addDeviceChangeListener(deviceListener)
        try {
            return future.get(1, TimeUnit.MINUTES)
        } catch (e: TimeoutException) {
            throw ExecutionException("Failed to launch emulator: timeout", e)
        } finally {
            AndroidDebugBridge.removeDeviceChangeListener(deviceListener)
        }
    }
}

private val log = logger<AndroidDevice>()

// TODO use gradle project data for this
private fun getAppMetadata(apk: File): Pair<String, String> {
    val output = ExecUtil.execAndGetOutput(GeneralCommandLine(AndroidToolkit.aapt!!.path, "dump", "badging", apk.path))
    val lines = output.stdoutLines
    if (lines.isEmpty()) throw ExecutionException("aapt returned empty data for '$apk'")
    if (!lines[0].startsWith("package: name")) throw ExecutionException("aapt returned no package name for '$apk'")

    val appId = lines[0].removePrefix("package: name='").substringBefore('\'')
    val activityLine = lines.find { it.startsWith("launchable-activity: name") }
        ?: throw ExecutionException("aapt returned no main activity name for '$apk'")
    val mainActivity = activityLine
        .removePrefix("launchable-activity: name='")
        .substringBefore('\'')
    return appId to mainActivity
}