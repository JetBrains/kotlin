/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManagerListener
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger

interface DebuggerListener : XDebuggerManagerListener {
    fun registerDebuggerConnection(
        configuration: RunConfigurationBase<*>,
        params: JavaParameters?,
        runnerSettings: RunnerSettings?
    ): DebuggerConnection?
}

class CoroutineDebuggerListener(val project: Project) : DebuggerListener {
    val log by logger

    override fun registerDebuggerConnection(
        configuration: RunConfigurationBase<*>,
        params: JavaParameters?,
        runnerSettings: RunnerSettings?
    ): DebuggerConnection? {
        val isExternalSystemRunConfiguration = configuration is ExternalSystemRunConfiguration
        val isGradleConfiguration = gradleConfiguration(configuration.type.id)
        val disableCoroutineAgent = KotlinDebuggerSettings.getInstance().debugDisableCoroutineAgent
        if (!disableCoroutineAgent && runnerSettings is DebuggingRunnerData) {
            if (isExternalSystemRunConfiguration && isGradleConfiguration)
                // gradle related logic in KotlinGradleCoroutineDebugProjectResolver
                return DebuggerConnection(project, configuration, params, runnerSettings, false)
            else
                return DebuggerConnection(project, configuration, params, runnerSettings, true)
        }
        return null
    }

    private fun gradleConfiguration(configurationName: String) =
        "GradleRunConfiguration" == configurationName || "KotlinGradleRunConfiguration" == configurationName
}