/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import java.nio.file.Path

interface Module : Dependency {
    val project: Project
    val moduleName: String

    /**
     * Compiler arguments in the format of the Kotlin compiler CLI
     */
    val additionalCompilationArguments: List<String>

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

    val defaultStrategyConfig: CompilerExecutionStrategyConfiguration

    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    fun compile(
        strategyConfig: CompilerExecutionStrategyConfiguration = defaultStrategyConfig,
        forceOutput: LogLevel? = null,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit = {},
        assertions: context(Module) CompilationOutcome.() -> Unit = {},
    ): CompilationResult

    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    fun compileIncrementally(
        sourcesChanges: SourcesChanges,
        strategyConfig: CompilerExecutionStrategyConfiguration = defaultStrategyConfig,
        forceOutput: LogLevel? = null,
        forceNonIncrementalCompilation: Boolean = false,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit = {},
        incrementalCompilationConfigAction: (IncrementalJvmCompilationConfiguration<*>) -> Unit = {},
        assertions: context(Module) CompilationOutcome.() -> Unit = {},
    ): CompilationResult
}