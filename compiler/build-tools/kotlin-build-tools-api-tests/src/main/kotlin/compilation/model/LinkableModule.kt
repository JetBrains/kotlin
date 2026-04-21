/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy

interface LinkableModule<O : BaseCompilationOperation, B : BaseCompilationOperation.Builder> {
    val defaultStrategyConfig: ExecutionPolicy

    fun link(
        strategyConfig: ExecutionPolicy = defaultStrategyConfig,
        forceOutput: LogLevel? = null,
        compilationConfigAction: (B) -> Unit = {},
        compilationAction: (O) -> Unit = {},
        assertions: context(Module<*, *, *>) CompilationOutcome.() -> Unit = {},
    ): CompilationResult
}
