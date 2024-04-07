/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel

fun CompilationOutcome.expectFailWithError(vararg expectedErrorLines: Regex) {
    expectFailWithError(expectedErrorLines.toSet())
}

fun CompilationOutcome.expectFailWithError(expectedErrorLines: Set<Regex>) {
    expectFail()
    assertLogContainsPatterns(LogLevel.ERROR, expectedErrorLines)
}