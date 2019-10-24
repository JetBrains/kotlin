/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.openapi.util.Disposer
import org.jetbrains.konan.execution.AndroidCommandLineState
import org.jetbrains.konan.execution.AndroidProcessHandler

class AndroidTestCommandLineState(
    configuration: MobileTestRunConfiguration,
    environment: ExecutionEnvironment
) : AndroidCommandLineState(configuration, environment) {
    private val testRunnerApk = configuration.getTestRunnerBundle(environment)
    private val testData = configuration.testData as AndroidTestRunConfigurationData

    init {
        consoleBuilder = object : TextConsoleBuilderImpl(project) {
            override fun createConsole() =
                SMTestRunnerConnectionUtil.createConsole(configuration.createTestConsoleProperties(environment.executor)).also {
                    Disposer.register(project, it)
                }
        }
    }

    override fun startProcess(): AndroidProcessHandler =
        device.installAndRunTests(apk, testRunnerApk, project, runTests = ::runTests)

    override fun startDebugProcess(): AndroidProcessHandler =
        device.installAndRunTests(apk, testRunnerApk, project, waitForDebugger = true, runTests = ::runTests)

    private fun runTests(testRunner: RemoteAndroidTestRunner, handler: AndroidProcessHandler) {
        handler.shouldHandleTermination = false

        val testSuite = testData.testSuite
        val testName = testData.testName
        if (testSuite != null && testName != null) {
            testRunner.setMethodName(testSuite, testName)
        } else if (testSuite != null) {
            testRunner.setClassName(testSuite)
        }

        testRunner.run(AndroidTestListener(handler))
    }
}