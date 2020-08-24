package com.jetbrains.mobile.execution.testing

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.jetbrains.mobile.execution.CompositeCommandLineState

class CompositeTestCommandLineState(
    environment: ExecutionEnvironment,
    configuration: MobileTestRunConfiguration,
    states: List<CommandLineState>
) : CompositeCommandLineState(environment, states) {
    init {
        consoleBuilder = CompositeTestConsoleBuilder(configuration, environment.executor)
    }

    override fun configureConsoleBuilder(consoles: List<ExecutionConsole>) {
        (consoleBuilder as CompositeTestConsoleBuilder).consoles = consoles.filterIsInstance<SMTRunnerConsoleView>()
    }
}
