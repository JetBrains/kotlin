/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings

/**
 * Installs coroutines debug agent and coroutines tab if `kotlinx-coroutines-debug` dependency is found
 */
@Suppress("IncompatibleAPI")
class CoroutineDebugConfigurationExtension : RunConfigurationExtension() {

    override fun isApplicableFor(configuration: RunConfigurationBase<*>) = coroutineDebuggerEnabled()

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T,
        params: JavaParameters?,
        runnerSettings: RunnerSettings?
    ) {
        if (runnerSettings is DebuggingRunnerData && configuration is RunConfigurationBase<*>) {
            configuration.project.coroutineConnectionListener.configurationStarting(configuration, params, runnerSettings)
        }
    }
}
