/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrCommandLineState
import org.jetbrains.konan.AndroidToolkit
import org.jetbrains.konan.MobileBundle
import java.io.File

class AndroidDevice(private val raw: IDevice) : Device(
    raw.serialNumber,
    raw.displayName,
    "Android",
    raw.version.apiString
) {
    override fun createState(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrCommandLineState =
        AndroidCommandLineState(configuration, environment)

    fun install(apk: File) {
        raw.installPackage(apk.absolutePath, true)
    }

    fun launch(appId: String, activity: String, waitForDebugger: Boolean) {
        val receiver = CollectingOutputReceiver()
        val options = if (waitForDebugger) "-D" else ""
        raw.executeShellCommand(
            "am start $options -n \"$appId/$activity\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", receiver
        )
        log.debug("Launched app with output: ${receiver.output}")
    }

    fun installAndLaunch(apk: File, project: Project, waitForDebugger: Boolean = false): AndroidProcessHandler {
        val handler = AndroidProcessHandler(raw)
        runBackgroundableTask(MobileBundle.message("run.preparing"), project, cancellable = false) { indicator ->
            indicator.isIndeterminate = false

            val (appId, activity) = getAppMetadata(apk)
            handler.appId = appId

            indicator.fraction = 0.2
            indicator.text = MobileBundle.message("run.installing")
            install(apk)
            handler.prepareForLaunch()

            indicator.fraction = 0.8
            indicator.text = MobileBundle.message("run.starting")
            launch(appId, activity, waitForDebugger)
        }
        return handler
    }

    companion object {
        private val log = logger<AndroidDevice>()

        private val IDevice.displayName: String
            get() =
                if (isEmulator) avdName?.replace('_', ' ') ?: "Unknown Emulator"
                else getProperty(IDevice.PROP_DEVICE_MODEL) ?: "Unknown Device"
    }
}

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