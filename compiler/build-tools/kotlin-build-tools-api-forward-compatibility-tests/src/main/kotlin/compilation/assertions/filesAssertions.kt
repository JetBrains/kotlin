/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * Equivalent to [assertNoCompiledSources] with an empty array/set
 */
context(module: Module)
fun CompilationOutcome.assertNoCompiledSources() {
    assertCompiledSources()
}

context(module: Module)
fun CompilationOutcome.assertCompiledSources(vararg expectedCompiledSources: String) {
    assertCompiledSources(expectedCompiledSources.toSet())
}

context(module: Module)
fun CompilationOutcome.assertCompiledSources(expectedCompiledSources: Set<String>) {
    val actualCompiledSources = parseCompilationSteps().flatten().toSet()
    val normalizedPaths = normalizeFileNames(expectedCompiledSources)
    assertEquals(normalizedPaths, actualCompiledSources) {
        """
            Compiled sources do not match. Set diff:
            Unexpected: ${actualCompiledSources - normalizedPaths}
            Missing: ${normalizedPaths - actualCompiledSources}
        
            Full sets:
        """.trimIndent()
    }
}

/**
 * Asserts the per-step compilation sets during incremental compilation.
 * Unlike [assertCompiledSources], this checks each IC iteration separately,
 * which is necessary for verifying monotonous compile set expansion behavior.
 *
 * @param steps each set contains file names expected in the corresponding compile iteration
 */
context(module: Module)
fun CompilationOutcome.assertCompilationSteps(vararg steps: Set<String>) {
    val actualSteps = parseCompilationSteps()
    val expectedSteps = steps.map { normalizeFileNames(it) }
    assertEquals(expectedSteps.size, actualSteps.size) {
        "Expected ${expectedSteps.size} compilation steps but got ${actualSteps.size}.\nActual steps: $actualSteps"
    }
    expectedSteps.zip(actualSteps).forEachIndexed { index, (expected, actual) ->
        assertEquals(expected, actual) {
            """
                Compilation step ${index + 1} does not match.
                Unexpected: ${actual - expected}
                Missing: ${expected - actual}
            """.trimIndent()
        }
    }
}

context(module: Module)
private fun normalizeFileNames(fileNames: Set<String>): Set<String> =
    fileNames.map { fileName ->
        module.sourcesDirectory.resolve(fileName)
            .relativeTo(module.project.projectDirectory)
            .toString()
    }.toSet()

private fun CompilationOutcome.parseCompilationSteps(): List<Set<String>> {
    requireLogLevel(LogLevel.DEBUG)
    return logLines.getValue(LogLevel.DEBUG)
        .map { it.removePrefix("[KOTLIN] ") }
        .filter { it.startsWith("compile iteration") }
        .map { line ->
            line.removePrefix("compile iteration: ").trim().split(", ").toSet()
        }
}

/**
 * Asserts that the compiler produces all files declared as expected outputs.
 * Unless there's explicit expected output for the module's Kotlin module files, the default matching [Module.moduleName] will be added automatically.
 */
context(module: Module)
fun CompilationOutcome.assertOutputs(vararg expectedOutputs: String) {
    assertOutputs(expectedOutputs.toSet())
}

/**
 * Asserts that the compiler produces all files declared as expected outputs.
 * Unless there's explicit expected output for the module's Kotlin module files, the default matching [Module.moduleName] will be added automatically.
 */
context(module: Module)
fun CompilationOutcome.assertOutputs(expectedOutputs: Set<String>) {
    val filesLeft = expectedOutputs.map { module.outputDirectory.resolve(it).relativeTo(module.outputDirectory) }
        .toMutableSet()
        .apply {
            if (none { it.fileName.toString().endsWith(".kotlin_module") }) {
                add(module.outputDirectory.resolve("META-INF/${module.moduleName}.kotlin_module").relativeTo(module.outputDirectory))
            }
        }
    val notDeclaredFiles = hashSetOf<Path>()
    for (file in module.outputDirectory.walk()) {
        if (!file.isRegularFile()) continue
        val currentFile = file.relativeTo(module.outputDirectory)
        filesLeft.remove(currentFile).also { wasPreviously ->
            if (!wasPreviously) notDeclaredFiles.add(currentFile)
        }
    }
    assert(filesLeft.isEmpty() && notDeclaredFiles.isEmpty()) {
        val errors = mutableListOf<String>()
        if (filesLeft.isNotEmpty()) {
            errors.add("The following files were declared as expected, but not actually produced: $filesLeft")
        }
        if (notDeclaredFiles.isNotEmpty()) {
            errors.add("The following files weren't declared as expected output files: $notDeclaredFiles")
        }
        errors.joinToString(separator = "\n")
    }
}
