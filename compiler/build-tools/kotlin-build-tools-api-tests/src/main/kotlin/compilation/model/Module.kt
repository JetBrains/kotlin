/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module.Companion.EXECUTION_TIMEOUT_SECONDS
import java.nio.file.Path

interface Module : Dependency {
    val project: Project
    val moduleName: String

    /**
     * Hook for any additional configuration for the compilation operations within this module.
     */
    val moduleCompilationConfigAction: (JvmCompilationOperation) -> Unit

    /**
     * Directory containing all the source (.kt) files of the module
     */
    val sourcesDirectory: Path

    /**
     * Directory containing all the files and directories that are produced during the compilation process
     */
    val buildDirectory: Path

    /**
     * Directory containing compiler output (e.g. `.class` and `.kotlin_module` files)
     */
    val outputDirectory: Path

    /**
     * Directory containing all the files and directories related to incremental compilation
     */
    val icWorkingDir: Path

    /**
     * Directory containing caches required for incremental compilation
     */
    val icCachesDir: Path

    val defaultStrategyConfig: ExecutionPolicy

    fun compile(
        strategyConfig: ExecutionPolicy = defaultStrategyConfig,
        forceOutput: LogLevel? = null,
        compilationConfigAction: (JvmCompilationOperation) -> Unit = {},
        assertions: context(Module) CompilationOutcome.() -> Unit = {},
    ): CompilationResult

    fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: ExecutionPolicy = defaultStrategyConfig,
        forceOutput: LogLevel? = null,
        forceNonIncrementalCompilation: Boolean = false,
        compilationConfigAction: (JvmCompilationOperation) -> Unit = {},
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationOptions) -> Unit = {},
        assertions: context(Module) CompilationOutcome.() -> Unit = {},
    ): CompilationResult

    /**
     * Executes the currently compiled code in a separate process.
     * In JVM terms, dependencies are automatically added to classpath, based on module configuration.
     * Default timeout is [EXECUTION_TIMEOUT_SECONDS]
     *
     * [mainClassFqn] is used to select the entry point.
     *
     * Use this to assert that the compiled code behaves as expected (no runtime exceptions, correct output)
     */
    fun executeCompiledClass(
        mainClassFqn: String,
        assertions: ExecutionOutcome.() -> Unit = {},
    ): ExecutionOutcome

    companion object {
        const val EXECUTION_TIMEOUT_SECONDS = 10L
    }
}
