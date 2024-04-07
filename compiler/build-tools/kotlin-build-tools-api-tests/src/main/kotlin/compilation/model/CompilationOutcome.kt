/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationResult

interface CompilationOutcome {
    val logLines: Map<LogLevel, List<String>>

    val uniqueLogLines: Map<LogLevel, Set<String>>

    fun requireLogLevel(logLevel: LogLevel)

    fun expectFail()

    fun expectCompilationResult(compilationResult: CompilationResult)
}