/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.junit.runner.Runner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.ParametersRunnerFactory
import org.junit.runners.parameterized.TestWithParameters
import java.io.File
import java.lang.reflect.Method

private class AutoMute(
    val file: String,
    val issue: String
)

private val DO_AUTO_MUTE: AutoMute? by lazy {
    val autoMuteFile = File("tests/automute")
    if (autoMuteFile.exists()) {
        val lines = autoMuteFile.readLines().filter { it.isNotBlank() }.map { it.trim() }
        AutoMute(
            lines.getOrNull(0) ?: error("A file path is expected in tne first line"),
            lines.getOrNull(1) ?: error("An issue description is the second line")
        )
    } else {
        null
    }
}

private class MutedTest(
    val key: String,
    @Suppress("unused") val issue: String?,
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

    fun mutedTest(testClass: Class<*>, methodName: String): MutedTest? {
        val mutedTests = cache[methodName]?.get(testClass.simpleName) ?: return null

        return mutedTests.firstOrNull { mutedTest ->
            testClass.canonicalName.endsWith(mutedTest.classNameKey)
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
    return isMutedInDatabase(testCase.javaClass, testCase.name)
}

fun isMutedInDatabase(method: Method): Boolean {
    return isMutedInDatabase(method.declaringClass, method.name)
}

fun isMutedInDatabase(testClass: Class<*>, methodName: String): Boolean {
    val mutedTest = mutedSet.mutedTest(testClass, methodName)
    return mutedTest != null && !mutedTest.hasFailFile
}

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): (() -> Unit)? {
    if (isMutedInDatabase(testCase)) {
        return {
            System.err.println(mutedMessage(testKey(testCase)))
        }
    }

    val doAutoMute = DO_AUTO_MUTE ?: return null

    return {
        try {
            f()
        } catch (e: Throwable) {
            val file = File(doAutoMute.file)
            val lines = file.readLines()
            val firstLine = lines[0]
            val muted = lines.drop(1).toMutableList()
            muted.add("${testKey(testCase)}, ${doAutoMute.issue}")
            val newMuted: List<String> = mutableListOf<String>() + firstLine + muted.sorted()
            file.writeText(newMuted.joinToString("\n"))

            throw e
        }
    }
}

private fun mutedMessage(key: String) = "MUTED TEST: $key"

private fun testKey(testCase: TestCase) = "${testCase::class.java.canonicalName}.${testCase.name}"
private fun testKey(method: Method) = "${method.declaringClass.canonicalName}.${method.name}"

class RunnerFactoryWithMuteInDatabase: ParametersRunnerFactory {
    override fun createRunnerForTestWithParameters(test: TestWithParameters?): Runner {
        return object : BlockJUnit4ClassRunnerWithParameters(test) {
            override fun isIgnored(child: FrameworkMethod): Boolean {
                return super.isIgnored(child) || isIgnoredInDatabaseWithLog(child)
            }
        }
    }
}

private fun isIgnoredInDatabaseWithLog(child: FrameworkMethod): Boolean {
    if (isMutedInDatabase(child.method)) {
        System.err.println(mutedMessage(testKey(child.method)))
        return true
    }

    return false
}

fun TestCase.runTest(test: () -> Unit) {
    (wrapWithMuteInDatabase(this, test) ?: test).invoke()
}