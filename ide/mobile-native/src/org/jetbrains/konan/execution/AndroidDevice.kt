/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.sdklib.AndroidVersion
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import org.jetbrains.konan.AndroidToolkit
import org.jetbrains.konan.MobileBundle
import java.io.File

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
            log.debug("Launched app with output: ${receiver.output}")
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
    override fun prepareDevice(): IDevice = DeviceService.instance.launchAndroidEmulator(this)
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