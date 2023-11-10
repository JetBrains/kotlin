/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Base class for compilation tests via the build tools API.
 *
 * All the tests marked by [CompilationTest] receive a [BuildRunnerProvider] as argument.
 * Effectively, the annotation makes the test parameterized. The test will be executed both within the daemon and in-process.
 *
 * If you are writing incremental compilation test, consider extending [BaseIncrementalCompilationTest]
 */
abstract class BaseCompilationTest<T : BaseScenarioDsl<*>> {
    @TempDir
    lateinit var workingDirectory: Path

    val project = Project()

    fun Module.compileImpl(
        runner: BuildRunner,
        dependencies: Set<Module> = emptySet(),
        assertions: (CompilationResult, Map<LogLevel, Collection<String>>) -> Unit = { _, _ -> },
    ) {
        val result = runner.compileModule(this, dependencies)

        if (buildToolsVersion >= KotlinToolingVersion(2, 0, 0, "Beta2")) {
            assertEquals(
                runner.selectedExecutionStrategy == ExecutionStrategy.IN_PROCESS,
                runner.testLogger.loggedMessages.getValue(LogLevel.DEBUG).contains("Compiling using the in-process strategy")
            )
            assertEquals(
                runner.selectedExecutionStrategy == ExecutionStrategy.DAEMON,
                runner.testLogger.loggedMessages.getValue(LogLevel.DEBUG).contains("Compiling using the daemon strategy")
            )
        }

        assertions(result, runner.testLogger.loggedMessages)
    }

    open fun maybeSkip(buildRunnerProvider: BuildRunnerProvider) {
    }

    abstract fun scenario(buildRunnerProvider: BuildRunnerProvider, init: T.() -> Unit)
}