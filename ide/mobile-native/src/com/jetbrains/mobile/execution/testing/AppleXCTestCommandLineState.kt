package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.testing.CidrRerunFailedTestsAction
import com.jetbrains.cidr.execution.testing.CidrTestCommandLineState
import com.jetbrains.cidr.execution.testing.CidrTestScope
import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.xctest.OCRerunFailedTestsAction
import com.jetbrains.cidr.execution.testing.xctest.OCUnitTestScopeElement
import com.jetbrains.mobile.execution.AppleDevice
import java.nio.file.Paths

class AppleXCTestCommandLineState(
    configuration: MobileTestRunConfiguration,
    device: AppleDevice,
    env: ExecutionEnvironment,
    executor: Executor,
    failedTests: CidrTestScope?
) : CidrTestCommandLineState<MobileTestRunConfiguration>(configuration, configuration.createLauncher(env, device), env, executor, failedTests, EMPTY_TEST_SCOPE_PRODUCER) {
    init {
        consoleBuilder = object : CidrConsoleBuilder(configuration.project, null, configuration.project.basePath?.let { Paths.get(it) }) {
            override fun createConsole() = this@AppleXCTestCommandLineState.createConsole(this)
        }
    }

    override fun createTestScopeElement(testClass: String?, testMethod: String?): CidrTestScopeElement =
        OCUnitTestScopeElement(testClass, testMethod)

    override fun doCreateRerunFailedTestsAction(consoleView: SMTRunnerConsoleView): CidrRerunFailedTestsAction =
        OCRerunFailedTestsAction(consoleView, this)

    override fun createConsole(builder: CidrConsoleBuilder): ConsoleView =
        builder.createConsole(myConfiguration.type, AppleXCTestConsoleProperties(configuration, executor, environment.executionTarget))

    companion object {
        private val EMPTY_TEST_SCOPE_PRODUCER = {
            CidrTestScope.createEmptyTestScope(AppleXCTestFramework.instance.patternSeparatorInCommandLine)
        }
    }
}