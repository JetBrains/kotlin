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
import com.jetbrains.mpp.KonanCommandLineState
import com.jetbrains.mpp.RunnerBase
import com.jetbrains.mpp.debugger.KonanExternalSystemState
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import java.io.File

internal const val ACTUAL_XC_PROJECT_FILE = "project.pbxproj"
internal const val DEBUG_INFORMATION_FORMAT_KEY = "DEBUG_INFORMATION_FORMAT"
internal const val DEBUG_INFORMATION_FORMAT_VALUE = "dwarf-with-dsym"


class AppleRunner : RunnerBase() {

    private val balloonNotification = NotificationGroup.balloonGroup("Xcode");

    override fun getRunnerId(): String = "AppleRunner"

    override fun getWorkspace(project: Project) = ProjectWorkspace.getInstance(project)

    override fun canRun(executorId: String, profile: RunProfile) = when (profile) {
        is BinaryRunConfiguration -> canRunBinary(executorId, profile)
        is AppleRunConfiguration -> true
        else -> false
    }

    private fun checkDSYMIsGenerated(configuration: AppleRunConfiguration?) {
        if (configuration == null) return
        val xcDir = configuration.xcProjectFile?.absolutePath ?: return

        // dSYMs are needed in the setting with CocoaPods, which implies that project is being governed by workspace
        if (xcDir.endsWith(XcFileExtensions.project)) return

        val xcProjectDir = (xcDir.removeSuffix(XcFileExtensions.workspace) + XcFileExtensions.project)
        val xcActualProjectFile = File(xcProjectDir).resolve(ACTUAL_XC_PROJECT_FILE)

        // do not trigger warning for nontrivial configuration
        if (!xcActualProjectFile.exists()) return

        val debugFormatSettings = xcActualProjectFile.readLines().filter { it.contains(DEBUG_INFORMATION_FORMAT_KEY) }

        if (!debugFormatSettings.all { it.contains(DEBUG_INFORMATION_FORMAT_VALUE) }) {
            balloonNotification.createNotification(
                XmlStringUtil.wrapInHtml(KonanBundle.message("label.informationAboutDSYM.title")),
                XmlStringUtil.wrapInHtml(KonanBundle.message("label.informationAboutDSYM.text")),
                NotificationType.INFORMATION, null
            ).notify(configuration.project)
        }
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.executor.id != DefaultDebugExecutor.EXECUTOR_ID) {
            if (state is CidrCommandLineState && state.executionTarget is ApplePhysicalDevice) {
                return contentDescriptor(environment, true) { session ->
                    state.startDebugProcess(session)
                }
            }

            return super.doExecute(state, environment)
        }

        if (state is CidrCommandLineState && environment.runProfile is AppleRunConfiguration) {
            checkDSYMIsGenerated(environment.runProfile as? AppleRunConfiguration)
        }

        return when (state) {
            is CidrCommandLineState -> contentDescriptor(environment) { session -> state.startDebugProcess(session) }
            is KonanCommandLineState -> contentDescriptor(environment) { session -> state.startDebugProcess(session) }
            is KonanExternalSystemState -> contentDescriptor(environment) { session -> state.startDebugProcess(session, environment) }
            else -> throw ExecutionException("RunProfileState  ${state.javaClass} is not supported by ${this.javaClass}")
        }
    }
}