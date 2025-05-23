/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private class CompilationOutcomeImpl(
    rawLogLines: Map<LogLevel, Collection<String>>,
    override val actualResult: CompilationResult
) : CompilationOutcome {
    private val _logLines by lazy {
        rawLogLines.mapValues { (_, lines) -> lines.toList() }
    }

    override val logLines: Map<LogLevel, List<String>>
        get() = _logLines

    private val _uniqueLogLines by lazy {
        rawLogLines.mapValues { (_, lines) -> lines.toSet() }
    }

    override val uniqueLogLines: Map<LogLevel, Set<String>>
        get() = _uniqueLogLines

    var maxLogLevel: LogLevel = LogLevel.ERROR
        private set
    var expectedResult = CompilationResult.COMPILATION_SUCCESS
        private set

    override fun requireLogLevel(logLevel: LogLevel) {
        maxLogLevel = maxOf(logLevel, maxLogLevel)
    }

    override fun expectFail() {
        expectedResult = CompilationResult.COMPILATION_ERROR
    }

    override fun expectCompilationResult(compilationResult: CompilationResult) {
        expectedResult = compilationResult
    }
}

data class AbstractModuleCacheKey(
    val moduleName: String,
    val dependencies: List<DependencyScenarioDslCacheKey>,
    val additionalCompilationArguments: List<String>,
) : DependencyScenarioDslCacheKey

abstract class AbstractModule(
    override val project: Project,
    final override val moduleName: String,
    val moduleDirectory: Path,
    val dependencies: List<Dependency>,
    override val defaultStrategyConfig: CompilerExecutionStrategyConfiguration,
    final override val additionalCompilationArguments: List<String> = emptyList(),
) : Module {
    override val sourcesDirectory: Path
        get() = moduleDirectory.resolve("src")

    override val buildDirectory: Path
        get() = moduleDirectory.resolve("build")

    override val outputDirectory: Path
        get() = buildDirectory.resolve("output")

    override val location: Path
        get() = outputDirectory

    override val icWorkingDir: Path
        get() = buildDirectory.resolve("ic")

    override val icCachesDir: Path
        get() = icWorkingDir.resolve("caches")

    override val scenarioDslCacheKey = AbstractModuleCacheKey(moduleName, dependencies.map { it.scenarioDslCacheKey }, additionalCompilationArguments)

    override fun compile(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        forceOutput: LogLevel?,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit,
        assertions: CompilationOutcome.(Module) -> Unit,
    ): CompilationResult {
        val kotlinLogger = TestKotlinLogger()
        val result = compileImpl(strategyConfig, compilationConfigAction, kotlinLogger)
        val outcome = CompilationOutcomeImpl(kotlinLogger.logMessagesByLevel, result)
        try {
            assertions(outcome, this)
            assertEquals(outcome.expectedResult, result) {
                "Compilation result is unexpected"
            }
            if (forceOutput != null) {
                kotlinLogger.printBuildOutput(forceOutput)
            }
        } catch (e: AssertionError) {
            val maxLogLevel = if (forceOutput != null) {
                maxOf(forceOutput, outcome.maxLogLevel)
            } else {
                outcome.maxLogLevel
            }
            kotlinLogger.printBuildOutput(maxLogLevel)
            throw e
        }
        return result
    }

    protected abstract fun compileImpl(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit,
        kotlinLogger: TestKotlinLogger,
    ): CompilationResult

    override fun executeCompiledClass(mainClassFqn: String, assertions: ExecutionOutcome.() -> Unit): ExecutionOutcome {
        val executionProcess = prepareExecutionProcessBuilder(mainClassFqn)
            .redirectErrorStream(true)
            .start()

        Runtime.getRuntime().addShutdownHook(Thread {
            executionProcess.destroyForcibly()
        })

        var executionError: Throwable? = null
        try {
            if (!executionProcess.waitFor(Module.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executionProcess.destroyForcibly()
                executionError = TimeoutException("process couldn't complete its work in time")
            }
        } catch (anyError: Throwable) {
            executionError = anyError
        }

        val outcome = ExecutionOutcome(
            output = executionProcess.inputStream.bufferedReader().readLines(),
            isComplete = executionError == null && executionProcess.exitValue() == 0,
            failureReason = executionError
        )
        try {
            outcome.apply { assertions() }
        } catch (e: AssertionError) {
            println("Error on Test runner side: ${outcome.failureReason?.toString() ?: "None"}")
            println("Full output of the execution process:\n${outcome.output.joinToString("\n")}")
            throw e
        }
        return outcome
    }

    protected abstract fun prepareExecutionProcessBuilder(
        mainClassFqn: String
    ): ProcessBuilder

    override fun toString() = moduleName
}
