package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.JavaTestLocator
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator

class AndroidTestConsoleProperties(
    configuration: RunConfiguration,
    executor: Executor
) : SMTRunnerConsoleProperties(configuration, "JUnit", executor) {

    override fun getTestLocator(): SMTestLocator? = JavaTestLocator.INSTANCE
}
