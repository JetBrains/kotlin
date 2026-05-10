/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.android.tests.emulator

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.android.tests.PathManager
import org.jetbrains.kotlin.android.tests.run.runProcessCancellable
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Emulator(private val pathManager: PathManager, private val platform: String?) {
    private val createCommand: GeneralCommandLine
        get() {
            val commandLine = GeneralCommandLine()
            val androidCmdName = if (SystemInfo.isWindows) "avdmanager.bat" else "avdmanager"
            commandLine.exePath = pathManager.toolsFolderInAndroidSdk + "/bin/" + androidCmdName
            commandLine.addParameters(
                "create", "avd", "--force", "-n", AVD_NAME, "-p", pathManager.getAndroidAvdRoot(), "-k"
            )

            // Allow override of system image via system property
            val overrideImage = System.getProperty("kotlin.android.avd.systemImage")
            if (overrideImage != null && !overrideImage.isEmpty()) {
                commandLine.addParameter(overrideImage)
            } else if (platform == X86) {
                commandLine.addParameter("system-images;android-$SYSTEM_IMAGE_API;default;x86_64")
            } else {
                commandLine.addParameter("system-images;android-$SYSTEM_IMAGE_API;default;arm64-v8a")
            }

            commandLine.withEnvironment("ANDROID_SDK_ROOT", pathManager.androidSdkRoot)
            commandLine.withEnvironment("ANDROID_HOME", pathManager.androidSdkRoot)

            return commandLine
        }

    private val startCommand: GeneralCommandLine
        get() {
            val commandLine = GeneralCommandLine()

            // Prefer the new SDK layout: $SDK/emulator/emulator
            val sdkRoot = pathManager.androidSdkRoot
            val newLayoutEmulator = "$sdkRoot/emulator/emulator"

            val newEmulatorFile = File(newLayoutEmulator)
            if (newEmulatorFile.isFile() && newEmulatorFile.canExecute()) {
                commandLine.exePath = newEmulatorFile.absolutePath
            } else {
                // Fallback to the old path if needed
                commandLine.exePath = pathManager.emulatorFolderInAndroidSdk + "/emulator"
            }

            commandLine.addParameters("-avd", AVD_NAME, "-no-audio", "-no-window", "-gpu", "swiftshader_indirect")
            if (isRunningInCi) {
                println("Disabling emulator hardware acceleration in CI")
                commandLine.addParameter("-no-accel")
            } else {
                println("Using emulator hardware acceleration for local run")
            }
            return commandLine
        }

    private val waitCommand: GeneralCommandLine
        get() {
            val commandLine = createAdbCommand()
            commandLine.addParameter("wait-for-device")
            return commandLine
        }

    private val stopCommandForAdb: GeneralCommandLine
        get() {
            val commandLine = createAdbCommand()
            commandLine.addParameter("kill-server")
            return commandLine
        }

    private fun patchAvdConfigSystemImage() {
        val configIni = File(pathManager.getAndroidAvdRoot(), "config.ini")
        if (!configIni.isFile()) return

        try {
            val lines = Files.readAllLines(configIni.toPath())
            val patched: MutableList<String?> = ArrayList(lines.size)
            var replaced = false

            for (line in lines) {
                if (line.startsWith("image.sysdir.1=")) {
                    // Force the correct relative path under SDK root
                    if (platform == X86) {
                        patched.add("image.sysdir.1=system-images/android-$SYSTEM_IMAGE_API/default/x86_64/")
                    } else {
                        patched.add("image.sysdir.1=system-images/android-$SYSTEM_IMAGE_API/default/arm64-v8a/")
                    }
                    replaced = true
                } else {
                    patched.add(line)
                }
            }

            if (!replaced) {
                // If the key was missing, add it explicitly
                if (platform == X86) {
                    patched.add("image.sysdir.1=system-images/android-$SYSTEM_IMAGE_API/default/x86_64/")
                } else {
                    patched.add("image.sysdir.1=system-images/android-$SYSTEM_IMAGE_API/default/arm64-v8a/")
                }
            }

            Files.write(configIni.toPath(), patched)
        } catch (e: IOException) {
            throw RuntimeException(e.message, e)
        }
    }

    suspend fun createEmulator() {
        println("Creating emulator...")
        runProcessCancellable(createCommand, stdin = "no\n")
        // Fix up stale system image path in config.ini, otherwise, there will be androidSdk/androidSdk in path.
        patchAvdConfigSystemImage()
    }

    private fun createAdbCommand(): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = pathManager.platformToolsFolderInAndroidSdk + "/" + "adb"
        return commandLine
    }

    suspend fun startServer() {
        val commandLine = createAdbCommand()
        commandLine.addParameter("start-server")
        println("Start adb server...")
        runProcessCancellable(commandLine)
    }

    suspend fun runEmulator() {
        println("Starting emulator with ANDROID_HOME/ANDROID_SDK_ROOT: " + pathManager.androidSdkRoot)
        val command = startCommand
        command.withEnvironment("ANDROID_SDK_ROOT", pathManager.androidSdkRoot)
        command.withEnvironment("ANDROID_HOME", pathManager.androidSdkRoot)

        runProcessCancellable(command)
    }

    suspend fun printLog() {
        val commandLine = createAdbCommand()
        commandLine.addParameters("logcat", "-v", "time", "-s", "dalvikvm:W", "TestRunner:I")
        runProcessCancellable(commandLine)
    }

    suspend fun waitEmulatorStart() {
        println("Waiting for emulator start...")

        withTimeout(androidStartupTimeout()) {
            runProcessCancellable(waitCommand)

            val bootCheckCommand = createAdbCommand()
            bootCheckCommand.addParameters("shell", "getprop", "sys.boot_completed")

            while (true) {
                val result = runProcessCancellable(
                    bootCheckCommand,
                    timeout = 30.seconds,
                    checkExitCode = false,
                )
                if (result.exitCode == 0 && result.stdout.trim() == "1") break
                delay(10.seconds)
            }

            println("Waiting for Package Manager...")
            val packageManagerCheckCommand = createAdbCommand()
            packageManagerCheckCommand.addParameters("shell", "pm", "path", "android")

            while (true) {
                val result = runProcessCancellable(
                    packageManagerCheckCommand,
                    timeout = 30.seconds,
                    checkExitCode = false,
                )
                if (result.exitCode == 0 && result.stdout.contains("package:")) break
                delay(10.seconds)
            }
        }
    }

    suspend fun waitForInstallStabilization() {
        val stabilizationDelay = if (isRunningInCi) 15.seconds else Duration.ZERO
        if (stabilizationDelay != Duration.ZERO) {
            println("Waiting ${stabilizationDelay.inWholeSeconds}s for emulator services to settle before install...")
            delay(stabilizationDelay)
        }
    }

    fun installRetryDelay(): Duration =
        if (isRunningInCi) 15.seconds else 5.seconds

    suspend fun dumpInstallDiagnostics(reason: String) {
        println("Dumping adb diagnostics: $reason")
        runDiagnosticCommand("devices", "-l")
        runDiagnosticCommand("shell", "getprop", "sys.boot_completed")
        runDiagnosticCommand("shell", "getprop", "dev.bootcomplete")
        runDiagnosticCommand("shell", "pm", "path", "android")
    }

    private fun androidStartupTimeout(): Duration {
        val minutes = System.getenv("kotlin.tests.android.timeout")?.toLongOrNull() ?: 45L
        return minutes.minutes
    }

    suspend fun stopAdbServer() {
        println("Stopping adb server...")
        runProcessCancellable(stopCommandForAdb)
    }

    suspend fun runTestsViaInstrumentation(suiteClassName: String?): String {
        println("Running tests via adb instrumentation for $suiteClassName...")
        val adbCommand = createAdbCommand()
        adbCommand.addParameters(
            "shell", "am", "instrument", "-w", "-r",
            "-e", "class", suiteClassName,
            "org.jetbrains.kotlin.android.tests.gradle/org.jetbrains.kotlin.android.tests.KotlinBoxInstrumentation"
        )
        val execute = runProcessCancellable(adbCommand)
        return execute.stdout
    }

    private suspend fun runDiagnosticCommand(vararg parameters: String) {
        val commandLine = createAdbCommand()
        commandLine.addParameters(*parameters)
        println("Diagnostic command: ${commandLine.commandLineString}")
        val result = runProcessCancellable(commandLine, checkExitCode = false)
        println("Diagnostic exit code: ${result.exitCode}")
    }

    companion object {
        const val ARM: String = "arm"
        const val X86: String = "x86"
        private const val AVD_NAME = "kotlin_box_test_avd"
        private const val SYSTEM_IMAGE_API = "26"

        private val isRunningInCi: Boolean
            get() = java.lang.Boolean.getBoolean("kotlin.test.android.teamcity")
                    || !StringUtil.isEmpty(System.getenv("TEAMCITY_VERSION"))
    }
}
