/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.ExecutionOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Basic check that the code execution completed (to avoid false results based on partial output)
 *
 * This should be asserted by "convenience" assertions, but complex tests might use it explicitly
 */
fun ExecutionOutcome.assertCompletion() {
    assertTrue(isComplete) { "Execution couldn't complete" }
}

/**
 * Useful if you're expecting a runtime error
 */
fun ExecutionOutcome.assertFailure() {
    assertFalse(isComplete) { "Execution didn't fail, unexpectedly" }
}

/**
 * Convenience assertion - use it when the code must succeed and print a specific line
 */
fun ExecutionOutcome.assertExactOutput(oneLine: String) {
    assertCompletion()

    assertEquals(1, output.count()) { "unexpected multi-line output:\n${output.joinToString("\n")}" }
    assertEquals(oneLine, output.singleOrNull()) { "wrong output" }
}
