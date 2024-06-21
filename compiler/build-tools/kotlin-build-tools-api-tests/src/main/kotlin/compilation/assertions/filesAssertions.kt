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
fun CompilationOutcome.assertNoCompiledSources(module: Module) {
    assertCompiledSources(module)
}

fun CompilationOutcome.assertCompiledSources(module: Module, vararg expectedCompiledSources: String) {
    assertCompiledSources(module, expectedCompiledSources.toSet())
}

fun CompilationOutcome.assertCompiledSources(module: Module, expectedCompiledSources: Set<String>) {
    requireLogLevel(LogLevel.DEBUG)
    val actualCompiledSources = logLines.getValue(LogLevel.DEBUG)
        .filter { it.startsWith("compile iteration") }
        .flatMap { it.replace("compile iteration: ", "").trim().split(", ") }
        .toSet()
    val normalizedPaths = expectedCompiledSources
        .map { module.sourcesDirectory.resolve(it) }
        .map { it.relativeTo(module.project.projectDirectory) }
        .map(Path::toString)
        .toSet()
    assertEquals(normalizedPaths, actualCompiledSources) {
        "Compiled sources do not match "
    }
}

fun CompilationOutcome.assertOutputs(module: Module, vararg expectedOutputs: String) {
    assertOutputs(module, expectedOutputs.toSet())
}

fun CompilationOutcome.assertOutputs(module: Module, expectedOutputs: Set<String>) {
    val filesLeft = expectedOutputs.map { module.outputDirectory.resolve(it).relativeTo(module.outputDirectory) }
        .toMutableSet()
        .apply {
            add(module.outputDirectory.resolve("META-INF/${module.moduleName}.kotlin_module").relativeTo(module.outputDirectory))
        }
    val notDeclaredFiles = hashSetOf<Path>()
    for (file in module.outputDirectory.walk()) {
        if (!file.isRegularFile()) continue
        val currentFile = file.relativeTo(module.outputDirectory)
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