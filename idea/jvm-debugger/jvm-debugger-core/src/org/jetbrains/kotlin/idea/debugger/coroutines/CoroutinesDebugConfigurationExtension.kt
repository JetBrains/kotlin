/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutines

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.diagnostic.Logger

/**
 * Installs coroutines debug agent and coroutines tab if `kotlinx.coroutines.debug` dependency is found
 */
@Suppress("IncompatibleAPI")
class CoroutinesDebugConfigurationExtension : RunConfigurationExtension() {
    private val log = Logger.getInstance(this::class.java)

    override fun isApplicableFor(configuration: RunConfigurationBase<*>) = isCoroutineDebuggerEnabled()

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T,
        params: JavaParameters?,
        runnerSettings: RunnerSettings?
    ) {
        if (!isCoroutineDebuggerEnabled()) return
        if (runnerSettings is DebuggingRunnerData) {
            try {
                val kotlinxCoroutinesClassPathLib = params?.classPath?.pathList?.first { it.contains("kotlinx-coroutines-debug") }
                initializeCoroutineAgent(params!!, kotlinxCoroutinesClassPathLib, configuration)
            } catch (e: NoSuchElementException) {
                log.warn("'kotlinx-coroutines-debug' not found in classpath. Coroutine debugger disabled.")
            }
        }
    }
}