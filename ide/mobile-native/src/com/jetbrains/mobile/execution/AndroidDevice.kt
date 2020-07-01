package com.jetbrains.mobile.execution

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.EmulatorConsole
import com.android.ddmlib.IDevice
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.android.sdklib.AndroidVersion
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.mobile.AndroidToolkit
import com.jetbrains.mobile.MobileBundle
import com.jetbrains.mobile.execution.testing.AndroidTestCommandLineState
import com.jetbrains.mobile.execution.testing.MobileTestRunConfiguration
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
    private inline fun execute(
        project: Project, isDebug: Boolean,
        crossinline block: (AndroidProcessHandler, ProgressIndicator) -> Unit
    ): AndroidProcessHandler {
        val handler = AndroidProcessHandler()
        runBackgroundableTask(MobileBundle.message("run.waiting"), project, cancellable = false) { indicator ->
            try {
                handler.raw = prepareDevice(project)

                indicator.isIndeterminate = false
                indicator.fraction = 0.1
                indicator.text = MobileBundle.message("run.preparing")

                block(handler, indicator)
            } catch (e: Throwable) {
                handler.destroyProcess()
                if (e is ExecutionException) {
                    val message = e.message ?: e.toString()
                    val window = if (isDebug) ToolWindowId.DEBUG else ToolWindowId.RUN
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

    fun installAndLaunch(apk: File, project: Project, waitForDebugger: Boolean = false): AndroidProcessHandler =
        execute(project, waitForDebugger) { handler, indicator ->
            val (appId, activity) = getAppMetadata(project, apk)
            handler.appId = appId

            indicator.fraction = 0.3
            indicator.text = MobileBundle.message("run.installing")

            handler.raw.installPackage(apk.absolutePath, true)
            handler.prepareForLaunch()

            indicator.fraction = 0.8
            indicator.text = MobileBundle.message("run.starting")

            val receiver = CollectingOutputReceiver()
            val options = if (waitForDebugger) "-D" else ""
            handler.raw.executeShellCommand(
                "am start $options -n \"$appId/$activity\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", receiver
            )
            log.info("Launched app with output: ${receiver.output.trimEnd()}")
        }

    fun installAndRunTests(
        appApk: File, testApk: File, project: Project,
        testInstrumentationRunner: String,
        waitForDebugger: Boolean = false,
        runTests: (RemoteAndroidTestRunner, AndroidProcessHandler) -> Unit
    ): AndroidProcessHandler =
        execute(project, waitForDebugger) { handler, indicator ->
            val testAppId = getAppId(project, testApk)
            handler.appId = getAppId(project, appApk)

            indicator.fraction = 0.3
            indicator.text = MobileBundle.message("run.installing")
            handler.raw.installPackage(appApk.absolutePath, true)

            indicator.fraction = 0.5
            indicator.text = MobileBundle.message("run.installing.tests")
            handler.raw.installPackage(testApk.absolutePath, true)

            handler.prepareForLaunch()

            indicator.fraction = 0.8
            indicator.text = MobileBundle.message("run.starting.tests")

            val testRunner = RemoteAndroidTestRunner(testAppId, testInstrumentationRunner, handler.raw)
            testRunner.setDebug(waitForDebugger)
            log.info("Running tests: ${testRunner.amInstrumentCommand}")

            ProcessIOExecutorService.INSTANCE.execute {
                try {
                    runTests(testRunner, handler)
                } catch (e: Throwable) {
                    log.error(e)
                }
            }
        }

    protected abstract fun prepareDevice(project: Project): IDevice
}

class AndroidPhysicalDevice(private val raw: IDevice) : AndroidDevice(
    raw.serialNumber,
    raw.getProperty(IDevice.PROP_DEVICE_MODEL) ?: "Unknown Device",
    raw.version
) {
    override fun prepareDevice(project: Project): IDevice = raw
}

class AndroidEmulator(avdName: String, osVersion: AndroidVersion?) : AndroidDevice(
    avdName,
    avdName.replace('_', ' '),
    osVersion
) {
    override fun prepareDevice(project: Project): IDevice {
        // If already running, return immediately
        (DeviceService.getInstance(project) as MobileDeviceService).adb?.devices?.find { it.avdName == id }?.let { return it }

        val commandLine = GeneralCommandLine(AndroidToolkit.getInstance(project).emulator!!.path, "-avd", id)
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
private fun aapt(project: Project, apk: File): List<String> {
    val output = ExecUtil.execAndGetOutput(GeneralCommandLine(AndroidToolkit.getInstance(project).aapt!!.path, "dump", "badging", apk.path))
    val lines = output.stdoutLines
    if (lines.isEmpty()) throw ExecutionException("aapt returned empty data for '$apk'")
    return lines
}

private fun getAppId(aaptOutput: List<String>): String {
    if (!aaptOutput[0].startsWith("package: name")) throw ExecutionException("aapt returned no package name")
    return aaptOutput[0].removePrefix("package: name='").substringBefore('\'')
}

private fun getAppId(project: Project, apk: File): String = getAppId(aapt(project, apk))

private fun getAppMetadata(project: Project, apk: File): Pair<String, String> {
    val lines = aapt(project, apk)
    val appId = getAppId(lines)
    val activityLine = lines.find { it.startsWith("launchable-activity: name") }
        ?: throw ExecutionException("aapt returned no main activity name for '$apk'")
    val mainActivity = activityLine
        .removePrefix("launchable-activity: name='")
        .substringBefore('\'')
    return appId to mainActivity
}