/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import org.jetbrains.konan.execution.AndroidCommandLineState

class AndroidTestCommandLineState(
    configuration: MobileTestRunConfiguration,
    environment: ExecutionEnvironment
) : AndroidCommandLineState(configuration, environment) {
    private val testRunnerApk = configuration.getTestRunnerBundle(environment)

    init {
        consoleBuilder = object : TextConsoleBuilderImpl(project) {
            override fun createConsole(): ConsoleView =
                SMTestRunnerConnectionUtil.createConsole(configuration.createTestConsoleProperties(environment.executor))
        }
    }

    override fun startProcess(): ProcessHandler {
        return device.installAndRunTests(apk, testRunnerApk, project) { testRunner, handler ->
            testRunner.run(AndroidTestListener(handler))
        }
    }
}