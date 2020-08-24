package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties

class CompositeTestConsoleProperties(
    configuration: MobileTestRunConfiguration,
    executor: Executor,
) : SMTRunnerConsoleProperties(configuration, "Mobile", executor)
