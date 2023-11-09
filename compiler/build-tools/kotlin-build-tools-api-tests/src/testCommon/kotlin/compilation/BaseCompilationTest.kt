/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunner
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.ExecutionStrategy
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.Module
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.Project
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*

abstract class BaseCompilationTest {
    @TempDir
    lateinit var workingDirectory: Path

    val project = Project()

    fun Module.compile(
        runner: BuildRunner,
        dependencies: Set<Module> = emptySet(),
        assertions: (Map<LogLevel, Collection<String>>) -> Unit = { _ -> },
    ) {
        compileImpl(runner, dependencies) { result, logs ->
            assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
            assertions(logs)
        }
    }

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
}