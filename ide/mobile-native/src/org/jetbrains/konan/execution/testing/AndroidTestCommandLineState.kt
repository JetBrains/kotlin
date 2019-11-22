/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Disposer
import org.jetbrains.konan.execution.AndroidCommandLineState
import org.jetbrains.konan.execution.AndroidProcessHandler
import org.jetbrains.konan.gradle.AndroidProjectResolver
import org.jetbrains.plugins.gradle.util.GradleUtil

class AndroidTestCommandLineState(
    configuration: MobileTestRunConfiguration,
    environment: ExecutionEnvironment
) : AndroidCommandLineState(configuration, environment) {
    private val testRunnerApk = configuration.getTestRunnerBundle(environment)
    private val testData = configuration.testData as AndroidTestRunConfigurationData
    private val testInstrumentationRunner =
        try {
            @Suppress("UnstableApiUsage")
            val moduleData = GradleUtil.findGradleModuleData(configuration.module!!)!!
            val model = ExternalSystemApiUtil.find(moduleData, AndroidProjectResolver.KEY)!!.data
            model.testInstrumentationRunner
        } catch (e: Throwable) {
            log.warn(e)
            "androidx.test.runner.AndroidJUnitRunner"
        }

    init {
        consoleBuilder = object : TextConsoleBuilderImpl(project) {
            override fun createConsole() =
                SMTestRunnerConnectionUtil.createConsole(configuration.createTestConsoleProperties(environment.executor)).also {
                    Disposer.register(project, it)
                }
        }
    }

    private fun start(waitForDebugger: Boolean): AndroidProcessHandler =
        device.installAndRunTests(apk, testRunnerApk, project, testInstrumentationRunner, waitForDebugger, runTests = ::runTests)

    override fun startProcess(): AndroidProcessHandler = start(waitForDebugger = false)
    override fun startDebugProcess(): AndroidProcessHandler = start(waitForDebugger = true)

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

    companion object {
        private val log = logger<AndroidTestCommandLineState>()
    }
}