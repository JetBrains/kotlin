/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * Equivalent to [assertNoCompiledSources] with an empty array/set
 */
context(Module)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertNoCompiledSources() {
    assertCompiledSources()
}

context(Module)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertCompiledSources(vararg expectedCompiledSources: String) {
    assertCompiledSources(expectedCompiledSources.toSet())
}

context(Module)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertCompiledSources(expectedCompiledSources: Set<String>) {
    requireLogLevel(LogLevel.DEBUG)
    val actualCompiledSources = logLines.getValue(LogLevel.DEBUG)
        .filter { it.startsWith("compile iteration") }
        .flatMap { it.replace("compile iteration: ", "").trim().split(", ") }
        .toSet()
    val normalizedPaths = expectedCompiledSources
        .map { sourcesDirectory.resolve(it) }
        .map { it.relativeTo(project.projectDirectory) }
        .map(Path::toString)
        .toSet()
    assertEquals(normalizedPaths, actualCompiledSources) {
        "Compiled sources do not match "
    }
}

context(Module)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertOutputs(vararg expectedOutputs: String) {
    assertOutputs(expectedOutputs.toSet())
}

context(Module)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertOutputs(expectedOutputs: Set<String>) {
    val filesLeft = expectedOutputs.map { outputDirectory.resolve(it).relativeTo(outputDirectory) }
        .toMutableSet()
        .apply {
            add(outputDirectory.resolve("META-INF/$moduleName.kotlin_module").relativeTo(outputDirectory))
        }
    val notDeclaredFiles = hashSetOf<Path>()
    for (file in outputDirectory.walk()) {
        if (!file.isRegularFile()) continue
        val currentFile = file.relativeTo(outputDirectory)
        filesLeft.remove(currentFile).also { wasPreviously ->
            if (!wasPreviously) notDeclaredFiles.add(currentFile)
        }
    }
    assert(filesLeft.isEmpty()) {
        "The following files were declared as expected, but not actually produced: $filesLeft"
    }
    assert(notDeclaredFiles.isEmpty()) {
        "The following files weren't declared as expected output files: $notDeclaredFiles"
    }
}