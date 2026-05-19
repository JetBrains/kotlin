/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.junit.jupiter.api.Assertions.assertEquals

private fun Collection<String>.parseCompilationSteps(): List<Set<String>> {
    return map { it.removePrefix("[KOTLIN] ") }
        .filter { it.startsWith("compile iteration") }
        .map { line ->
            line.removePrefix("compile iteration: ")
                .trim()
                .split(", ")
                .map { it.replace('\\', '/') }
                .toSet()
        }
}

fun Collection<String>.assertCompiledSources(expectedCompiledSources: Set<String>) {
    val actualCompiledSources = parseCompilationSteps().flatten().toSet()
    assertEquals(expectedCompiledSources, actualCompiledSources) {
        """
            Compiled sources do not match. Set diff:
            Unexpected: ${actualCompiledSources - expectedCompiledSources}
            Missing: ${expectedCompiledSources - actualCompiledSources}
        """.trimIndent()
    }
}
