package com.jetbrains.mobile.execution

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.util.SmartList

open class CompositeCommandLineState(
    environment: ExecutionEnvironment,
    val states: List<CommandLineState>
) : CommandLineState(environment) {
    init {
        assert(states.isNotEmpty())
        assert(states.none { it is CompositeCommandLineState })
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val handlers = SmartList<ProcessHandler>()
        val consoles = SmartList<ExecutionConsole>()
        for (state in states) {
            val result = state.execute(executor, runner)
            handlers += result.processHandler
            consoles += result.executionConsole
        }
        val compositeHandler = CompositeProcessHandler(handlers)
        val console = consoles.singleOrNull()
            ?: {
                configureConsoleBuilder(consoles)
                val console = createConsole(executor)!!
                console.attachToProcess(compositeHandler)
                console
            }()
        return DefaultExecutionResult(console, compositeHandler)
    }

    protected open fun configureConsoleBuilder(consoles: List<ExecutionConsole>) {}

    override fun startProcess(): ProcessHandler = throw IllegalStateException()
}
