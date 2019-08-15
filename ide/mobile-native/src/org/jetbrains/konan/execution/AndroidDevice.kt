/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.testing.CidrLauncher
import org.jetbrains.konan.AndroidToolkit
import org.jetbrains.konan.MobileBundle
import java.io.File
import java.io.OutputStream

class AndroidDevice(private val raw: IDevice) : Device(
    raw.serialNumber,
    raw.avdName?.replace('_', ' ') ?: "Unknown",
    "Android",
    raw.version.apiString
) {
    override fun createState(configuration: MobileRunConfiguration, environment: ExecutionEnvironment): CidrCommandLineState =
        AndroidCommandLineState(configuration, environment)

    fun install(apk: File) {
        raw.installPackage(apk.absolutePath, true)
    }

    fun launch(appId: String, activity: String) {
        val receiver = CollectingOutputReceiver()
        raw.executeShellCommand(
            "am start -n \"$appId/$activity\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", receiver
        )
        log.debug("Installed app with output: ${receiver.output}")
    }

    fun installAndLaunch(apk: File, project: Project): ProcessHandler {
        val handler = AndroidProcessHandler(raw)
        runBackgroundableTask(MobileBundle.message("run.preparing"), project, cancellable = false) { indicator ->
            indicator.isIndeterminate = false

            val (appId, activity) = getAppMetadata(apk)
            handler.appId = appId

            indicator.fraction = 0.2
            indicator.text = MobileBundle.message("run.installing")
            install(apk)

            indicator.fraction = 0.8
            indicator.text = MobileBundle.message("run.starting")
            launch(appId, activity)
        }
        return handler
    }
}

private class AndroidCommandLineState(
    val configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment
) : CidrCommandLineState(environment, FakeLauncher()) {
    private val device = environment.executionTarget as AndroidDevice

    override fun startProcess(): ProcessHandler {
        val apk = configuration.getProductBundle(environment)
        return device.installAndLaunch(apk, configuration.project)
    }

    override fun startDebugProcess(session: XDebugSession): XDebugProcess {
        TODO("not implemented")
    }
}

private class AndroidProcessHandler(
    val raw: IDevice
) : ProcessHandler() {
    lateinit var appId: String
    private var processClient: Client? = null

    private fun isRelevantEvent(device: IDevice, changeMask: Int, expectedMask: Int) =
        (changeMask and expectedMask) == expectedMask &&
                device.serialNumber == raw.serialNumber

    private val clientListener = object : AndroidDebugBridge.IClientChangeListener {
        override fun clientChanged(client: Client, changeMask: Int) {
            if (!isRelevantEvent(client.device, changeMask, Client.CHANGE_NAME)) return

            synchronized(this@AndroidProcessHandler) {
                if (appId == client.clientData.clientDescription ||
                    appId == client.clientData.packageName
                ) {
                    processClient = client
                }
                notifyTextAvailable(MobileBundle.message("run.android.started", client.clientData.pid) + "\n", ProcessOutputType.SYSTEM)
            }
        }
    }

    private val deviceListener = object : AndroidDebugBridge.IDeviceChangeListener {
        override fun deviceChanged(device: IDevice, changeMask: Int) {
            if (!isRelevantEvent(device, changeMask, IDevice.CHANGE_CLIENT_LIST)) return

            synchronized(this@AndroidProcessHandler) {
                val client = device.getClient(appId)
                if (client != null) {
                    processClient = client
                    return
                }

                if (processClient != null) {
                    detachProcess()
                }
            }
        }

        override fun deviceConnected(device: IDevice?) {}
        override fun deviceDisconnected(device: IDevice?) {}
    }

    init {
        AndroidDebugBridge.addClientChangeListener(clientListener)
        AndroidDebugBridge.addDeviceChangeListener(deviceListener)

        addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                notifyTextAvailable(MobileBundle.message("run.android.finished") + "\n", ProcessOutputType.SYSTEM)
            }
        })
    }

    override fun getProcessInput(): OutputStream? = null
    override fun detachIsDefault(): Boolean = false

    override fun detachProcessImpl() {
        notifyProcessDetached()
        cleanup()
    }

    override fun destroyProcessImpl() {
        val receiver = CollectingOutputReceiver()
        raw.executeShellCommand("am force-stop $appId", receiver)
        log.debug("Destroyed process with output: ${receiver.output}")
        notifyProcessTerminated(0)
        cleanup()
    }

    private fun cleanup() {
        AndroidDebugBridge.removeClientChangeListener(clientListener)
        AndroidDebugBridge.removeDeviceChangeListener(deviceListener)
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

private class FakeLauncher : CidrLauncher() {
    private fun error(): Nothing = throw IllegalStateException("this function should never be called")

    override fun getProject(): Project = error()
    override fun createProcess(state: CommandLineState): ProcessHandler = error()
    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess = error()
}

private val log = logger<AndroidDevice>()