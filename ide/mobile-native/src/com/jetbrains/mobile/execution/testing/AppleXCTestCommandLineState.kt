package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
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
    env: ExecutionEnvironment,
    executor: Executor,
    failedTests: CidrTestScope?
) : CidrTestCommandLineState<MobileTestRunConfiguration>(configuration, configuration.createLauncher(env, configuration.executionTargets.filterIsInstance<AppleDevice>().first()), env, executor, failedTests, EMPTY_TEST_SCOPE_PRODUCER) {
    init {
        consoleBuilder = object : CidrConsoleBuilder(configuration.project, null, configuration.project.basePath?.let { Paths.get(it) }) {
            override fun createConsole() = this@AppleXCTestCommandLineState.createConsole(this)
        }
    }

    override fun createTestScopeElement(testClass: String?, testMethod: String?): CidrTestScopeElement =
        OCUnitTestScopeElement(testClass, testMethod)

    override fun doCreateRerunFailedTestsAction(consoleView: SMTRunnerConsoleView): CidrRerunFailedTestsAction =
        OCRerunFailedTestsAction(consoleView, this)

    companion object {
        private val EMPTY_TEST_SCOPE_PRODUCER = {
            CidrTestScope.createEmptyTestScope(AppleXCTestFramework.instance.patternSeparatorInCommandLine)
        }
    }
}