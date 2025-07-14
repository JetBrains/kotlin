/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.impl.valueOfOrNull
import java.io.File

data class SteppingTestLoggedData(val line: Int, val isSynthetic: Boolean, val expectation: String)

sealed interface LocalValue

class LocalPrimitive(val value: String, val valueType: String) : LocalValue {
    override fun toString(): String {
        return "$value:$valueType"
    }
}

class LocalReference(val id: String, val referenceType: String) : LocalValue {
    override fun toString(): String {
        return referenceType
    }
}

object LocalNullValue : LocalValue {
    override fun toString(): String {
        return "null"
    }
}

class LocalVariableRecord(
    val variable: String,
    val variableType: String?,
    val value: LocalValue
) {
    override fun toString(): String = buildString {
        append(variable)
        if (variableType != null) {
            append(":")
            append(variableType)
        }
        append("=")
        append(value.toString().normalizeIndyLambdas())
    }
}

private fun String.normalizeIndyLambdas(): String =
    // Invokedynamic lambdas have an unstable hash in the name.
    replace("\\\$Lambda\\\$.*".toRegex(), "<lambda>")

private const val EXPECTATIONS_MARKER = "// EXPECTATIONS"
private const val FORCE_STEP_INTO_MARKER = "// FORCE_STEP_INTO"
private const val DIRECTIVE_MARKER = "+"

data class BackendWithDirectives(val backend: TargetBackend) {
    companion object {
        private val directivesToConsider = mutableSetOf(LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS)
    }

    private val directives = mutableSetOf<Directive>()

    fun addDirectiveIfConsidered(directive: Directive) {
        if (directive in directivesToConsider) {
            directives += directive
        }
    }

    fun contains(registeredDirectives: RegisteredDirectives, directivesInTestFile: Set<Directive>): Boolean {
        if (directivesInTestFile.isEmpty()) return true
        return registeredDirectives.filter { it in directivesToConsider && it in directivesInTestFile }.toSet() == directives
    }
}

fun checkSteppingTestResult(
    frontendKind: FrontendKind<*>,
    targetBackend: TargetBackend,
    wholeFile: File,
    loggedItems: List<SteppingTestLoggedData>,
    directives: RegisteredDirectives
) {
    val actual = mutableListOf<String>()
    val lines = wholeFile.readLines()
    val directivesInTestFile = mutableSetOf<Directive>()
    var forceStepInto = false
    for (line in lines) {
        if (line.contains(DIRECTIVE_MARKER)) {
            directivesInTestFile.addAll(line.getDeclaredDirectives())
        }
        if (line.startsWith(FORCE_STEP_INTO_MARKER)) {
            forceStepInto = true
        }
    }

    val actualLineNumbers = compressSequencesWithoutLineNumber(loggedItems)
        .filter {
            // Ignore synthetic code with no line number information unless force step into behavior is requested.
            forceStepInto || !it.isSynthetic
        }
        .map { "// ${it.expectation}" }
    val actualLineNumbersIterator = actualLineNumbers.iterator()

    val lineIterator = lines.listIterator()
    for (line in lineIterator) {
        if (line.startsWith(EXPECTATIONS_MARKER)) {
            // Rewind the iterator to the first '// EXPECTATIONS' line
            if (lineIterator.hasPrevious()) lineIterator.previous()
            break
        }
        actual.add(line)
        if (line.startsWith(FORCE_STEP_INTO_MARKER)) break
    }

    var currentBackends = listOf(BackendWithDirectives(TargetBackend.ANY))
    var currentFrontends = listOf(frontendKind)
    for (line in lineIterator) {
        if (line.isEmpty()) {
            actual.add(line)
            continue
        }
        if (line.startsWith(EXPECTATIONS_MARKER)) {
            actual.add(line)
            val options = line.removePrefix(EXPECTATIONS_MARKER).splitToSequence(Regex("\\s+")).filter { it.isNotEmpty() }
            val backends = mutableListOf<BackendWithDirectives>()
            val frontends = mutableListOf<FrontendKind<*>>()
            var currentBackendWithDirectives: BackendWithDirectives? = null
            for (option in options) {
                val backend = valueOfOrNull<TargetBackend>(option)
                if (backend != null) {
                    val backendWithDirectives = BackendWithDirectives(backend)
                    currentBackendWithDirectives = backendWithDirectives
                    backends += backendWithDirectives
                    continue
                }

                val frontend = FrontendKinds.fromString(option)
                if (frontend != null) {
                    frontends += frontend
                    continue
                }

                val directive = LanguageSettingsDirectives[option.substringAfter(DIRECTIVE_MARKER)]
                if (directive != null && currentBackendWithDirectives != null) {
                    currentBackendWithDirectives.addDirectiveIfConsidered(directive)
                }
            }

            currentBackends = backends.takeIf { it.isNotEmpty() } ?: listOf(BackendWithDirectives(TargetBackend.ANY))
            currentFrontends = frontends.takeIf { it.isNotEmpty() } ?: listOf(frontendKind)
            continue
        }

        val containsBackend =
            currentBackends.any {
                it.backend == TargetBackend.ANY || (it.backend == targetBackend && it.contains(directives, directivesInTestFile))
            }
        if (containsBackend && currentFrontends.contains(frontendKind)) {
            if (actualLineNumbersIterator.hasNext()) {
                actual.add(actualLineNumbersIterator.next())
            }
        } else {
            actual.add(line)
        }
    }

    actualLineNumbersIterator.forEach { actual.add(it) }
    if (actual.last().isNotBlank()) {
        actual.add("")
    }

    assertEqualsToFile(wholeFile, actual.joinToString("\n"))
}

private fun String.getDeclaredDirectives(): List<Directive> {
    return split(Regex("\\s+")).mapNotNull { LanguageSettingsDirectives[it.substringAfter(DIRECTIVE_MARKER)] }
}

/**
 * Compresses sequences of the same location without line number in the log:
 * specifically removes locations without linenumber, that would otherwise
 * print as byte offsets. This avoids overspecifying code generation
 * strategy in debug tests.
 */
private fun compressSequencesWithoutLineNumber(loggedItems: List<SteppingTestLoggedData>): List<SteppingTestLoggedData> {
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

fun formatAsSteppingTestExpectation(
    sourceName: String,
    lineNumber: Int?,
    functionName: String,
    isSynthetic: Boolean,
    visibleVars: List<LocalVariableRecord>? = null
): String = buildString {
    append(sourceName)
    append(':')
    if (lineNumber != null) {
        append(lineNumber)
    } else {
        append("...")
    }
    append(' ')
    append(functionName)
    if (isSynthetic)
        append(" (synthetic)")
    if (visibleVars != null) {
        append(": ")
        visibleVars.joinTo(this)
    }
}.trim()
