/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.*
import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.logcat.LogCatReceiverTask
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.konan.MobileBundle
import java.io.OutputStream

class AndroidProcessHandler(private val raw: IDevice) : ProcessHandler() {
    lateinit var appId: String
    private var processClient: Client? = null
    private val logCatTask = LogCatReceiverTask(raw)

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
                notifyTextAvailable(
                    MobileBundle.message("run.android.started", client.clientData.pid) + "\n",
                    ProcessOutputType.SYSTEM
                )
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

    fun prepareForLaunch() {
        AndroidDebugBridge.addClientChangeListener(clientListener)
        AndroidDebugBridge.addDeviceChangeListener(deviceListener)

        logCatTask.addLogCatListener { messages: List<LogCatMessage> ->
            messages
                .filter { it.appName == appId }
                .forEach {
                    notifyTextAvailable(
                        "${it.logLevel.priorityLetter}/${it.tag}: ${it.message}\n",
                        if (it.logLevel >= Log.LogLevel.ERROR) ProcessOutputType.STDERR
                        else ProcessOutputType.STDOUT
                    )
                }
        }
        AppExecutorUtil.getAppExecutorService().execute(logCatTask)

        addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                logCatTask.stop()

                notifyTextAvailable(
                    MobileBundle.message("run.android.finished") + "\n",
                    ProcessOutputType.SYSTEM
                )

                AndroidDebugBridge.removeClientChangeListener(clientListener)
                AndroidDebugBridge.removeDeviceChangeListener(deviceListener)
            }
        })
    }

    override fun getProcessInput(): OutputStream? = null
    override fun detachIsDefault(): Boolean = false

    override fun detachProcessImpl() {
        notifyProcessDetached()
    }

    override fun destroyProcessImpl() {
        val receiver = CollectingOutputReceiver()
        raw.executeShellCommand("am force-stop $appId", receiver)
        log.debug("Destroyed process with output: ${receiver.output}")
        notifyProcessTerminated(0)
    }

    companion object {
        private val log = logger<AndroidProcessHandler>()
    }
}