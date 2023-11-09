/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import java.nio.file.Path

interface Module : Dependency {
    val moduleName: String
    val additionalCompilationArguments: List<String>
    val sourcesDirectory: Path
    val buildDirectory: Path
    val outputDirectory: Path
    val icWorkingDir: Path
    val icCachesDir: Path

    fun compile(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit = {},
    ): CompilationResult

    fun compileIncrementally(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        sourcesChanges: SourcesChanges,
        forceNonIncrementalCompilation: Boolean = false,
        compilationConfigAction: (JvmCompilationConfiguration) -> Unit = {},
    ): CompilationResult
}