package com.jetbrains.mobile.execution.testing

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.jetbrains.cidr.execution.testing.CidrTestRunConfiguration
import com.jetbrains.cidr.execution.testing.CidrTestRunConfigurationData
import com.jetbrains.cidr.execution.testing.CidrTestScope

class CompositeTestRunConfigurationData(
    configuration: MobileTestRunConfiguration,
    var testDatas: List<CidrTestRunConfigurationData<MobileTestRunConfiguration>>
) : CidrTestRunConfigurationData<MobileTestRunConfiguration>(configuration) {

    override fun getTestingFrameworkId(): String? = "Mobile"

    override fun formatTestMethod(): String = "$testSuite.$testName"

    override fun createState(environment: ExecutionEnvironment, executor: Executor, testScope: CidrTestScope?): CommandLineState? {
        val states = testDatas.mapNotNull { it.createState(environment, executor, testScope) }
        return CompositeTestCommandLineState(environment, myConfiguration, states)
    }

    override fun createTestConsoleProperties(executor: Executor, executionTarget: ExecutionTarget): SMTRunnerConsoleProperties =
        throw IllegalStateException()

    override fun clone(): CompositeTestRunConfigurationData {
        val result = super.clone() as CompositeTestRunConfigurationData
        result.testDatas = testDatas.map { it.cloneForConfiguration(myConfiguration) as CidrTestRunConfigurationData<MobileTestRunConfiguration> }
        return result
    }

    override fun cloneForConfiguration(configuration: CidrTestRunConfiguration): CompositeTestRunConfigurationData =
        super.cloneForConfiguration(configuration) as CompositeTestRunConfigurationData

    override fun setTestSuite(testSuite: String?) {
        testDatas.forEach { it.setTestSuite(testSuite) }
    }

    override fun setTestName(testName: String?) {
        testDatas.forEach { it.setTestName(testName) }
    }

    override fun setTestMode(mode: Mode) {
        testDatas.forEach { it.setTestMode(mode) }
    }

    override fun setCommandLineTestPattern(commandLineTestPattern: String?) {
        testDatas.forEach { it.setCommandLineTestPattern(commandLineTestPattern) }
    }

    override fun setTestPattern(testPattern: String?) {
        testDatas.forEach { it.setTestPattern(testPattern) }
    }

    override fun checkData() {
        testDatas.forEach { it.checkData() }
    }
}
