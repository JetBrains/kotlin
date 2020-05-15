/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.ParametersRunnerFactory
import org.junit.runners.parameterized.TestWithParameters
import java.io.File

private class AutoMute(
    val file: String,
    val issue: String
)

private val SKIP_MUTED_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.skip.muted.tests")

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

private fun AutoMute.muteTest(testKey: String) {
    val file = File(file)
    val lines = file.readLines()
    val firstLine = lines[0] // Drop file header
    val muted = lines.drop(1).toMutableList()
    muted.add("$testKey, $issue")
    val newMuted: List<String> = mutableListOf<String>() + firstLine + muted.sorted()
    file.writeText(newMuted.joinToString("\n"))
}

private class MutedTest(
    val key: String,
    @Suppress("unused") val issue: String?,
    val hasFailFile: Boolean,
    val isFlaky: Boolean
) {
    val methodKey: String
    val classNameKey: String
    val simpleClassName: String

    init {
        val noQuoteKey = key.replace("`", "")
        val beforeParamsKey = noQuoteKey.substringBefore("[")
        val params = noQuoteKey.substringAfterWithDelimiter("[", "")

        methodKey = (beforeParamsKey.substringAfterLast(".", "") + params)
            .also {
                if (it.isEmpty()) throw IllegalArgumentException("Can't get method name: '$key'")
            }

        classNameKey = beforeParamsKey.substringBeforeLast(".", "").also {
            if (it.isEmpty()) throw IllegalArgumentException("Can't get class name: '$key'")
        }

        simpleClassName = classNameKey.substringAfterLast(".")
    }

    companion object {
        fun String.substringAfterWithDelimiter(delimiter: String, missingDelimiterValue: String = this): String {
            val index = indexOf(delimiter)
            return if (index == -1) missingDelimiterValue else (delimiter + substring(index + 1, length))
        }
    }
}

private class MutedSet(muted: List<MutedTest>) {
    // Method key -> Simple class name -> List of muted tests
    private val cache: Map<String, Map<String, List<MutedTest>>> =
        muted
            .groupBy { it.methodKey } // Method key -> List of muted tests
            .mapValues { (_, tests) -> tests.groupBy { it.simpleClassName } }

    fun mutedTest(testClass: Class<*>, methodKey: String): MutedTest? {
        val mutedTests = cache[methodKey]?.get(testClass.simpleName) ?: return null

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

private val COLUMN_PARSE_REGEXP = Regex("\\s*(?:(?:\"((?:[^\"]|\"\")*)\")|([^,]*))\\s*")
private val MUTE_LINE_PARSE_REGEXP = Regex("$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP")

private fun parseMutedTest(str: String): MutedTest {
    val matchResult = MUTE_LINE_PARSE_REGEXP.matchEntire(str) ?: throw ParseError("Can't parse the line: $str")
    val resultValues = matchResult.groups.filterNotNull()

    val testKey = resultValues[1].value
    val issue = resultValues[2].value
    val stateStr = resultValues[3].value
    val statusStr = resultValues[4].value

    val hasFailFile = when (stateStr) {
        "MUTE", "" -> false
        "FAIL" -> true
        else -> throw ParseError("Invalid state (`$stateStr`), MUTE, FAIL or empty are expected: $str")
    }
    val isFlaky = when (statusStr) {
        "FLAKY" -> true
        "" -> false
        else -> throw ParseError("Invalid status (`$statusStr`), FLAKY or empty are expected: $str")
    }

    return MutedTest(testKey, issue, hasFailFile, isFlaky)
}

private class ParseError(message: String, override val cause: Throwable? = null) : IllegalArgumentException(message)

private val mutedSet by lazy {
    loadMutedSet(
        listOf(
            File("tests/mute-common.csv"),
            File("tests/mute-platform.csv")
        )
    )
}

internal fun isMutedInDatabase(testCase: TestCase): Boolean {
    return isMutedInDatabase(testCase.javaClass, testCase.name)
}

fun isMutedInDatabase(testClass: Class<*>, methodKey: String): Boolean {
    val mutedTest = mutedSet.mutedTest(testClass, methodKey)
    return mutedTest != null && (if (SKIP_MUTED_TESTS) !mutedTest.hasFailFile else mutedTest.isFlaky)
}

private fun getMutedTestOrNull(testCase: TestCase): MutedTest? {
    return getMutedTestOrNull(testCase.javaClass, testCase.name)
}

private fun getMutedTestOrNull(testClass: Class<*>, methodKey: String): MutedTest? {
    return mutedSet.mutedTest(testClass, methodKey)
}

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): (() -> Unit)? {
    if (isMutedInDatabase(testCase)) {
        return {
            System.err.println(mutedMessage(testKey(testCase)))
        }
    }

    val mutedTest = getMutedTestOrNull(testCase)

    if (mutedTest != null && !mutedTest.hasFailFile) {
        val testKey = testKey(testCase)
        return {
            var isTestGreen = true
            try {
                f()
            } catch (e: Throwable) {
                println("MUTED TEST STILL FAILS: $testKey")
                isTestGreen = false
            }
            if (isTestGreen) {
                System.err.println("SUCCESS RESULT OF MUTED TEST: $testKey")
                throw Exception("Muted non-flaky test $testKey finished successfully. Please remove it from csv file")
            }
        }
    } else {
        val doAutoMute = DO_AUTO_MUTE ?: return null
        return {
            try {
                f()
            } catch (e: Throwable) {
                doAutoMute.muteTest(testKey(testCase))
                throw e
            }
        }
    }


}

private fun mutedMessage(key: String) = "MUTED TEST: $key"

private fun testKey(klass: Class<*>, methodKey: String) = "${klass.canonicalName}.$methodKey"
private fun testKey(testCase: TestCase) = testKey(testCase::class.java, testCase.name)

class RunnerFactoryWithMuteInDatabase : ParametersRunnerFactory {
    override fun createRunnerForTestWithParameters(test: TestWithParameters?): Runner {
        return object : BlockJUnit4ClassRunnerWithParameters(test) {
            override fun isIgnored(child: FrameworkMethod): Boolean {
                return super.isIgnored(child) || isIgnoredInDatabaseWithLog(child, name)
            }

            override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
                notifier.withMuteFailureListener(method.declaringClass, parametrizedMethodKey(method, name)) {
                    super.runChild(method, notifier)
                }
            }
        }
    }
}

private fun parametrizedMethodKey(child: FrameworkMethod, parametersName: String): String {
    return child.method.name + parametersName
}

private inline fun RunNotifier.withMuteFailureListener(
    declaredClass: Class<*>,
    methodKey: String,
    crossinline run: () -> Unit,
) {
    val doAutoMute = DO_AUTO_MUTE
    if (doAutoMute == null) {
        run()
        return
    }

    val muteFailureListener = object : RunListener() {
        override fun testFailure(failure: Failure) {
            doAutoMute.muteTest(testKey(declaredClass, methodKey))
            super.testFailure(failure)
        }
    }

    try {
        addListener(muteFailureListener)
        run()
    } finally {
        removeListener(muteFailureListener)
    }
}

fun isIgnoredInDatabaseWithLog(child: FrameworkMethod): Boolean {
    if (isMutedInDatabase(child.declaringClass, child.name)) {
        System.err.println(mutedMessage(testKey(child.declaringClass, child.name)))
        return true
    }

    return false
}

fun isIgnoredInDatabaseWithLog(child: FrameworkMethod, parametersName: String): Boolean {
    if (isIgnoredInDatabaseWithLog(child)) {
        return true
    }

    val methodWithParametersKey = parametrizedMethodKey(child, parametersName)
    if (isMutedInDatabase(child.declaringClass, methodWithParametersKey)) {
        System.err.println(mutedMessage(testKey(child.declaringClass, methodWithParametersKey)))
        return true
    }

    return false
}

fun isIgnoredInDatabaseWithLog(testCase: TestCase): Boolean {
    if (isMutedInDatabase(testCase)) {
        System.err.println(mutedMessage(testKey(testCase)))
        return true
    }

    return false
}

fun TestCase.runTest(test: () -> Unit) {
    (wrapWithMuteInDatabase(this, test) ?: test).invoke()
}
