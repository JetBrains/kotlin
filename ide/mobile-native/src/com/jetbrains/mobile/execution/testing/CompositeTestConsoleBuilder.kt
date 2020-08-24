package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.util.Disposer

class CompositeTestConsoleBuilder(val configuration: MobileTestRunConfiguration, val executor: Executor) :
    TextConsoleBuilderImpl(configuration.project) {

    var consoles: List<SMTRunnerConsoleView> = emptyList()

    override fun createConsole(): ConsoleView {
        val properties = CompositeTestConsoleProperties(configuration, executor)
        val console = CompositeTestConsoleView(properties, consoles)
        Disposer.register(project, console)
        return console
    }
}
