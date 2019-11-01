/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import java.io.File

private class MutedTest(
    val key: String,
    val issue: String?,
    val hasFailFile: Boolean
) {
    val methodName = key.substringAfterLast(".", "").replace("`", "").also {
        if (it.isEmpty()) throw IllegalArgumentException("Can't get method name")
    }
    val classNameKey = key.substringBeforeLast(".", "").also {
        if (it.isEmpty()) throw IllegalArgumentException("Can't get class name")
    }
    val simpleClassName = classNameKey.substringAfterLast(".")
}

private class MutedSet(muted: List<MutedTest>) {
    // Method name -> Simple class name -> List of muted tests
    private val cache: Map<String, Map<String, List<MutedTest>>> =
        muted
            .groupBy { it.methodName } // Method name -> List of muted tests
            .mapValues { (_, tests) -> tests.groupBy { it.simpleClassName } }

    fun mutedTest(testCase: TestCase): MutedTest? {
        val mutedTests = cache[testCase.name]?.get(testCase.javaClass.simpleName) ?: return null

        return mutedTests.firstOrNull { mutedTest ->
            testCase.javaClass.canonicalName.endsWith(mutedTest.classNameKey)
        }
    }
}

private fun loadMutedSet(files: List<File>): MutedSet {
    return MutedSet(files.flatMap { file -> loadMutedTests(file) })
}

private fun loadMutedTests(file: File): List<MutedTest> {
    if (!file.exists()) {
        System.err.println("Can't find mute file: ${file.absolutePath}")
        return listOf()
    }

    try {
        val testLines = file.readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        return testLines.drop(1).map { parseMutedTest(it) }
    } catch (ex: Throwable) {
        throw ParseError("Couldn't parse file with muted tests: $file", cause = ex)
    }
}

private fun parseMutedTest(str: String): MutedTest {
    val components = str.splitToSequence(",").map { it.trim() }.toList()

    val key = components.getOrNull(0) ?: throw ParseError("Test key is absent (1 column): $str")

    val issue = components.getOrNull(1)

    val hasFailFile = when (val stateStr = components.getOrNull(2)) {
        "MUTE", "", null -> false
        "FAIL" -> true
        else -> throw ParseError("Invalid state (`$stateStr`), MUTE, FAIL or empty are expected: $str")
    }

    return MutedTest(key, issue, hasFailFile)
}

private class ParseError(message: String, override val cause: Throwable? = null) : IllegalArgumentException(message)

private val mutedSet by lazy {
    loadMutedSet(
        listOf(
            File("tests/mute.csv")
        )
    )
}

internal fun isMutedInDatabase(testCase: TestCase): Boolean {
    val mutedTest = mutedSet.mutedTest(testCase)
    return mutedTest != null && !mutedTest.hasFailFile
}

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): () -> Unit {
    if (!isMutedInDatabase(testCase)) return f
    return { MUTED_DO_TEST_LAMBDA(testCase) }
}

internal val MUTED_DO_TEST_LAMBDA = { testCase: TestCase ->
    System.err.println("MUTED TEST: ${testCase::class.java.name}.${testCase.name}")
}

internal class MutedDoTest(val testCase: TestCase) : KotlinTestUtils.DoTest {
    override fun invoke(filePath: String) {
        MUTED_DO_TEST_LAMBDA(testCase)
    }
}

fun TestCase.runTest(test: () -> Unit) {
    wrapWithMuteInDatabase(this, test).invoke()
}