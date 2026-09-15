/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ModuleContext
import org.jetbrains.kotlin.buildtools.tests.compilation.util.runProcess
import java.io.File

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
context(module: ModuleContext)
fun assertClassDeclarationsContain(classFqn: String, expectedDeclarations: Set<String>) {
    val actualDeclarations = classDeclarations(classFqn)
    assert((expectedDeclarations - actualDeclarations).isEmpty()) {
        declarationsMismatchMessage(expectedDeclarations, actualDeclarations)
    }
}

/**
 * Asserts that a given class declares exactly the expected declarations.
 *
 * @param classFqn The fully qualified name of the class to inspect.
 * @param expectedDeclarations The set of expected class declarations.
 */
context(module: ModuleContext)
fun assertClassDeclarations(classFqn: String, expectedDeclarations: Set<String>) {
    val actualDeclarations = classDeclarations(classFqn)
    assert(expectedDeclarations == actualDeclarations) {
        declarationsMismatchMessage(expectedDeclarations, actualDeclarations)
    }
}

context(module: ModuleContext)
private fun classDeclarations(classFqn: String): Set<String> {
    val javaHome = System.getProperty("java.home")
    val javapPath = File(javaHome, "bin/javap").let {
        // in case we got java.home pointing to the JRE part, javap is located in the outer JDK part
        if (it.exists()) it else File(javaHome, "../bin/javap")
    }.absolutePath
    val result = runProcess(listOf(javapPath, classFqn), module.outputDirectory)
    assert(result.isSuccessful) {
        "Failed to run javap on $classFqn.\n\n${result.output}"
    }

    return result.output.lines().map { it.trim() }.filter { it.isNotEmpty() }.drop(2).dropLast(1).toSet()
}

private fun declarationsMismatchMessage(expectedDeclarations: Set<String>, actualDeclarations: Set<String>): String {
    val expectedDeclarationsString = expectedDeclarations.joinToString(separator = "\n", prefix = "Expected declarations:\n")
    val actualDeclarationsString = actualDeclarations.joinToString(separator = "\n", prefix = "Actual declarations:\n")
    return "$expectedDeclarationsString\n\n$actualDeclarationsString"
}
