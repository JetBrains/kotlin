package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties

class AndroidTestConsoleProperties(
    configuration: RunConfiguration,
    executor: Executor
) : SMTRunnerConsoleProperties(configuration, "JUnit", executor)