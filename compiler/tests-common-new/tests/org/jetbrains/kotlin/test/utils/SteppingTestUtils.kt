/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import java.io.File

data class SteppingTestLoggedData(val line: Int, val isSynthetic: Boolean, val expectation: String)

private const val EXPECTATIONS_MARKER = "// EXPECTATIONS"
private const val FORCE_STEP_INTO_MARKER = "// FORCE_STEP_INTO"
private const val JVM_EXPECTATIONS_MARKER = "$EXPECTATIONS_MARKER JVM"
private const val JVM_IR_EXPECTATIONS_MARKER = "$EXPECTATIONS_MARKER JVM_IR"
private const val CLASSIC_FRONTEND_EXPECTATIONS_MARKER = "$EXPECTATIONS_MARKER CLASSIC_FRONTEND"
private const val FIR_EXPECTATIONS_MARKER = "$EXPECTATIONS_MARKER FIR"

fun checkSteppingTestResult(
    frontendKind: FrontendKind<*>,
    targetBackend: TargetBackend,
    wholeFile: File,
    loggedItems: List<SteppingTestLoggedData>
) {
    val actual = mutableListOf<String>()
    val lines = wholeFile.readLines()
    val forceStepInto = lines.any { it.startsWith(FORCE_STEP_INTO_MARKER) }

    val actualLineNumbers = compressSequencesWithoutLinenumber(loggedItems)
        .filter {
            // Ignore synthetic code with no line number information unless force step into behavior is requested.
            forceStepInto || !it.isSynthetic
        }
        .map { "// ${it.expectation}" }
    val actualLineNumbersIterator = actualLineNumbers.iterator()

    val lineIterator = lines.iterator()
    for (line in lineIterator) {
        actual.add(line)
        if (line.startsWith(EXPECTATIONS_MARKER) || line.startsWith(FORCE_STEP_INTO_MARKER)) break
    }

    var currentBackend = TargetBackend.ANY
    var currentFrontend = frontendKind
    for (line in lineIterator) {
        if (line.isEmpty()) {
            actual.add(line)
            continue
        }
        if (line.startsWith(EXPECTATIONS_MARKER)) {
            actual.add(line)
            currentBackend = when (line) {
                EXPECTATIONS_MARKER -> TargetBackend.ANY
                JVM_EXPECTATIONS_MARKER -> TargetBackend.JVM
                JVM_IR_EXPECTATIONS_MARKER -> TargetBackend.JVM_IR
                CLASSIC_FRONTEND_EXPECTATIONS_MARKER -> currentBackend
                FIR_EXPECTATIONS_MARKER -> currentBackend
                else -> error("Expected JVM backend: $line")
            }
            currentFrontend = when (line) {
                EXPECTATIONS_MARKER -> frontendKind
                JVM_EXPECTATIONS_MARKER -> currentFrontend
                JVM_IR_EXPECTATIONS_MARKER -> currentFrontend
                CLASSIC_FRONTEND_EXPECTATIONS_MARKER -> FrontendKinds.ClassicFrontend
                FIR_EXPECTATIONS_MARKER -> FrontendKinds.FIR
                else -> error("Expected JVM backend: $line")
            }
            continue
        }
        if ((currentBackend == TargetBackend.ANY || currentBackend == targetBackend) &&
            currentFrontend == frontendKind
        ) {
            if (actualLineNumbersIterator.hasNext()) {
                actual.add(actualLineNumbersIterator.next())
            }
        } else {
            actual.add(line)
        }
    }

    actualLineNumbersIterator.forEach { actual.add(it) }

    assertEqualsToFile(wholeFile, actual.joinToString("\n"))
}

/**
 * Compresses sequences of the same location without line number in the log:
 * specifically removes locations without linenumber, that would otherwise
 * print as byte offsets. This avoids overspecifying code generation
 * strategy in debug tests.
 */
private fun compressSequencesWithoutLinenumber(loggedItems: List<SteppingTestLoggedData>): List<SteppingTestLoggedData> {
    if (loggedItems.isEmpty()) return listOf()

    val logIterator = loggedItems.iterator()
    var currentItem = logIterator.next()
    val result = mutableListOf(currentItem)

    for (logItem in logIterator) {
        if (currentItem.line != -1 || currentItem.expectation != logItem.expectation) {
            result.add(logItem)
            currentItem = logItem
        }
    }

    return result
}

fun formatAsSteppingTestExpectation(sourceName: String, lineNumber: Int, functionName: String, isSynthetic: Boolean): String {
    val synthetic = if (isSynthetic) " (synthetic)" else ""
    return "$sourceName:$lineNumber $functionName$synthetic"
}
