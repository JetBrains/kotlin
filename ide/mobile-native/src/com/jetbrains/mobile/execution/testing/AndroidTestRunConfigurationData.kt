package com.jetbrains.mobile.execution.testing

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.testing.CidrTestRunConfigurationData
import com.jetbrains.cidr.execution.testing.CidrTestScope

class AndroidTestRunConfigurationData(configuration: MobileTestRunConfiguration) :
    CidrTestRunConfigurationData<MobileTestRunConfiguration>(configuration) {

    override fun getTestingFrameworkId(): String = "JUnit"

    override fun createTestConsoleProperties(executor: Executor, executionTarget: ExecutionTarget): AndroidTestConsoleProperties =
        AndroidTestConsoleProperties(myConfiguration, executor)

    override fun createState(environment: ExecutionEnvironment, executor: Executor, testScope: CidrTestScope?): CommandLineState =
        throw IllegalStateException()

    override fun formatTestMethod(): String = "$testSuite.$testName"

    override fun checkData() {}
}