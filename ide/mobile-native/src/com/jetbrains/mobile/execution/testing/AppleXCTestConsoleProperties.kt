package com.jetbrains.mobile.execution.testing

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.jetbrains.cidr.execution.testing.xctest.OCUnitConsoleProperties

class AppleXCTestConsoleProperties(
    configuration: MobileTestRunConfiguration,
    executor: Executor, target: ExecutionTarget
) : OCUnitConsoleProperties(configuration, executor, target) {

    override fun createTestEventsConverter(
        testFrameworkName: String,
        properties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter = AppleXCTestOutputToGeneralTestEventsConverter(this, testFrameworkName, properties)
}