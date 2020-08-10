package com.jetbrains.mobile.execution.testing

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.testing.CidrTestRunConfigurationData
import com.jetbrains.cidr.execution.testing.CidrTestScope
import com.jetbrains.mobile.execution.AndroidDevice

class AndroidTestRunConfigurationData(configuration: MobileTestRunConfiguration) :
    CidrTestRunConfigurationData<MobileTestRunConfiguration>(configuration) {

    override fun getTestingFrameworkId(): String = "JUnit"

    override fun createTestConsoleProperties(executor: Executor, executionTarget: ExecutionTarget): AndroidTestConsoleProperties =
        AndroidTestConsoleProperties(myConfiguration, executor)

    override fun createState(environment: ExecutionEnvironment, executor: Executor, testScope: CidrTestScope?): CommandLineState? {
        val device = myConfiguration.executionTargets.filterIsInstance<AndroidDevice>().firstOrNull()
            ?: return null
        return AndroidTestCommandLineState(myConfiguration, device, environment)
    }

    override fun formatTestMethod(): String = "$testSuite.$testName"

    override fun checkData() {}
}