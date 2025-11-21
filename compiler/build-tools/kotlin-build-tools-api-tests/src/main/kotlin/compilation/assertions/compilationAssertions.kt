/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.runProcess

fun CompilationOutcome.expectFailWithError(vararg expectedErrorLines: Regex) {
    expectFailWithError(expectedErrorLines.toSet())
}

fun CompilationOutcome.expectFailWithError(expectedErrorLines: Set<Regex>) {
    expectFail()
    assertLogContainsPatterns(LogLevel.ERROR, expectedErrorLines)
}

/**
 * Asserts that the class declarations of a given class contain the expected declarations. Uses `javap` to extract those.
 *
 * @param classesDir The path to the directory containing the compiled classes.
 * @param classFqn The fully qualified name of the class to inspect.
 * @param expectedDeclarations The set of expected class declarations.
 */
context(module: Module)
fun assertClassDeclarationsContain(classFqn: String, expectedDeclarations: Set<String>) {
    val javapPath = "${System.getenv("JAVA_HOME")}/bin/javap"
    val result = runProcess(listOf(javapPath, classFqn), module.outputDirectory)
    assert(result.isSuccessful) {
        "Failed to run javap on $classFqn.\n\n${result.output}"
    }
    val actualDeclarations = result.output.lines().drop(2).dropLast(1).map { it.trim() }.toSet()
    val diff = expectedDeclarations - actualDeclarations
    assert(diff.isEmpty()) {
        val expectedDeclarationsString = expectedDeclarations.joinToString(separator = "\n", prefix = "Expected declarations:\n")
        val actualDeclarationsString = actualDeclarations.joinToString(separator = "\n", prefix = "Actual declarations:\n")
        "$expectedDeclarationsString\n\n$actualDeclarationsString"
    }
}