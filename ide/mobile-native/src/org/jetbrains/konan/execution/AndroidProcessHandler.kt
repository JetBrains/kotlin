/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.*
import com.android.ddmlib.logcat.LogCatListener
import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.logcat.LogCatReceiverTask
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.konan.MobileBundle
import java.io.OutputStream

class AndroidProcessHandler(private val raw: IDevice) : ProcessHandler() {
    lateinit var appId: String
    private var processClient: Client? = null
    private val logCatTask = LogCatReceiverTask(raw)

    val debuggerPort: Int?
        @Synchronized get() = processClient?.debuggerListenPort

    private fun isRelevantEvent(device: IDevice, changeMask: Int, expectedMask: Int) =
        (changeMask and expectedMask) == expectedMask &&
                device.serialNumber == raw.serialNumber

    private val clientListener = object : AndroidDebugBridge.IClientChangeListener {
        override fun clientChanged(client: Client, changeMask: Int) {
            if (!isRelevantEvent(client.device, changeMask, Client.CHANGE_NAME)) return

            synchronized(this@AndroidProcessHandler) {
                if (appId == client.clientData?.clientDescription ||
                    appId == client.clientData?.packageName
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
                if (device.getClient(appId) == null && processClient != null) {
                    detachProcess()
                }
            }
        }

        override fun deviceConnected(device: IDevice?) {}
        override fun deviceDisconnected(device: IDevice?) {}
    }

    private val logCatListener = object : LogCatListener {
        // While the app is starting messages from it might arrive without `appName`
        // so we defer processing these messages until we know the app's pid
        var fromUnknownApp: MutableList<LogCatMessage>? = ArrayList()

        fun dispatchUnknownIfNeeded(pid: Int) {
            (fromUnknownApp ?: return)
                .forEach {
                    if (it.pid == pid) {
                        notify(it)
                    }
                }
            fromUnknownApp = null
        }

        fun notify(message: LogCatMessage) {
            notifyTextAvailable(
                "${message.logLevel.priorityLetter}/${message.tag}: ${message.message}\n",
                if (message.logLevel >= Log.LogLevel.ERROR) ProcessOutputType.STDERR
                else ProcessOutputType.STDOUT
            )
        }

        override fun log(messages: List<LogCatMessage>) {
            messages.forEach {
                if (it.appName == appId) {
                    dispatchUnknownIfNeeded(it.pid)
                    notify(it)
                } else if (it.appName == "?") {
                    fromUnknownApp?.add(it)
                }
            }
        }
    }

    fun prepareForLaunch() {
        AndroidDebugBridge.addClientChangeListener(clientListener)
        AndroidDebugBridge.addDeviceChangeListener(deviceListener)

        logCatTask.addLogCatListener(logCatListener)
        AppExecutorUtil.getAppExecutorService().execute(logCatTask)

        addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                logCatTask.stop()

                notifyTextAvailable(MobileBundle.message("run.android.finished") + "\n", ProcessOutputType.SYSTEM)

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
        processClient?.kill()
        notifyProcessTerminated(0)
    }

    companion object {
        private val log = logger<AndroidProcessHandler>()
    }
}