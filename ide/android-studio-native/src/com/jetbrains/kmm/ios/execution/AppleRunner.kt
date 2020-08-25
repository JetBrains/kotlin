/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.xml.util.XmlStringUtil
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.kmm.ios.AppleRunConfiguration
import com.jetbrains.kmm.ios.ProjectWorkspace
import com.jetbrains.kmm.ios.XcFileExtensions
import com.jetbrains.konan.KonanBundle
import com.jetbrains.mobile.execution.ApplePhysicalDevice
import com.jetbrains.mpp.BinaryDebugRunner
import java.io.File
import java.nio.file.Path

class AppleRunner : BinaryDebugRunner() {
    override fun getRunnerId(): String = "AppleRunner"
    override fun getWorkspace(project: Project) = ProjectWorkspace.getInstance(project)

    override fun canRun(executorId: String, profile: RunProfile) = when (profile) {
        is AppleRunConfiguration -> true
        else -> super.canRun(executorId, profile)
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val runConfiguration = environment.runProfile

        if (runConfiguration is AppleRunConfiguration) {
            state as? CidrCommandLineState ?: throw ExecutionException("${state.javaClass} doesn't support AppleRunConfiguration")
            val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID

            return when {
                isDebug -> {
                    checkDSYMIsGenerated(runConfiguration)
                    showDebugContent(environment) { state.startDebugProcess(it) }
                }
                runConfiguration.executionTarget is ApplePhysicalDevice -> {
                    showDebugContent(environment, true) { state.startDebugProcess(it) }
                }
                else -> {
                    showRunContent(state.execute(environment.executor, this), environment)
                }
            }
        } else return super.doExecute(state, environment)
    }

    private fun checkDSYMIsGenerated(configuration: AppleRunConfiguration) {
        val xcDir = configuration.xcProjectFile?.absolutePath ?: return

        // dSYMs are needed in the setting with CocoaPods, which implies that project is being governed by workspace
        if (xcDir.endsWith(XcFileExtensions.project)) return

        val xcProjectDir = (xcDir.removeSuffix(XcFileExtensions.workspace) + XcFileExtensions.project)
        val xcActualProjectFile = File(xcProjectDir).resolve(ACTUAL_XC_PROJECT_FILE)

        // do not trigger warning for nontrivial configuration
        if (!xcActualProjectFile.exists()) return

        val debugFormatSettings = xcActualProjectFile.readLines().filter { it.contains(DEBUG_INFORMATION_FORMAT_KEY) }

        if (!debugFormatSettings.all { it.contains(DEBUG_INFORMATION_FORMAT_VALUE) }) {
            NotificationGroup.balloonGroup("Xcode").createNotification(
                XmlStringUtil.wrapInHtml(KonanBundle.message("label.informationAboutDSYM.title")),
                XmlStringUtil.wrapInHtml(KonanBundle.message("label.informationAboutDSYM.text")),
                NotificationType.INFORMATION, null
            ).notify(configuration.project)
        }
    }

    override fun getPythonBindingsPath(project: Project): Path? = bundledBindingsPath

    companion object {
        private const val ACTUAL_XC_PROJECT_FILE = "project.pbxproj"
        private const val DEBUG_INFORMATION_FORMAT_KEY = "DEBUG_INFORMATION_FORMAT"
        private const val DEBUG_INFORMATION_FORMAT_VALUE = "dwarf-with-dsym"

        val bundledBindingsPath: Path by lazy {
            val outOfPluginPrettyPrinters = createTempDir().resolve("konan_lldb.py")
            outOfPluginPrettyPrinters.outputStream().use { outputStream ->
                AppleRunner::class.java.getResourceAsStream("/scripts/konan_lldb.py").copyTo(outputStream)
            }

            outOfPluginPrettyPrinters.toPath()
        }
    }
}