/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.junit.Assert
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.lang.Error
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class TestsExceptionFilePostfix(val text: String) {
    COMPILER_ERROR("compiler"),
    COMPILETIME_ERROR("compiletime"),
    RUNTIME_ERROR("runtime"),
    INFRASTRUCTURE_ERROR("infrastructure")
}

sealed class TestsError(val postfix: TestsExceptionFilePostfix) : Error() {
    abstract val original: Throwable
    override fun toString() = original.toString()
}

class TestsCompilerError(override val original: Throwable) : TestsError(TestsExceptionFilePostfix.COMPILER_ERROR)
class TestsInfrastructureError(override val original: Throwable) : TestsError(TestsExceptionFilePostfix.INFRASTRUCTURE_ERROR)
class TestsCompiletimeError(override val original: Throwable) : TestsError(TestsExceptionFilePostfix.COMPILETIME_ERROR)
class TestsRuntimeError(override val original: Throwable) : TestsError(TestsExceptionFilePostfix.RUNTIME_ERROR)

private enum class ExceptionType {
    ANALYZING_EXPRESSION,
    UNKNOWN
}

class TestExceptionsComparator(wholeFile: File) {
    companion object {
        private const val EXCEPTIONS_FILE_PREFIX = "exceptions"

        private val exceptionMessagePatterns = mapOf(
            ExceptionType.ANALYZING_EXPRESSION to
                    Pattern.compile("""Exception while analyzing expression at \((?<lineNumber>\d+),(?<symbolNumber>\d+)\) in /(?<filename>.*?)$""")
        )
        private val ls = System.lineSeparator()
    }

    private val filePathPrefix = "${wholeFile.parent}/${wholeFile.nameWithoutExtension}.$EXCEPTIONS_FILE_PREFIX"

    private fun analyze(e: Throwable): Matcher? {
        for ((_, pattern) in exceptionMessagePatterns) {
            if (e.message == null) continue
            val matches = pattern.matcher(e.message)
            if (matches.find()) return matches
        }

        return null
    }

    private fun getExceptionInfo(e: TestsError, cases: Set<Int>?): String {
        val casesAsString = cases?.run { "CASES: " + joinToString() + ls } ?: ""

        return when (e) {
            is TestsRuntimeError ->
                (e.original.cause ?: e.original).run {
                    casesAsString + toString() + stackTrace[0]?.let { ls + it }
                }
            is TestsCompilerError, is TestsInfrastructureError -> casesAsString + (e.original.cause ?: e.original).toString()
            is TestsCompiletimeError -> throw e.original
        }
    }

    private fun validateExistingExceptionFiles(e: TestsError?) {
        val postfixesOfFilesToCheck = TestsExceptionFilePostfix.values().toMutableSet().filter { it != e?.postfix }

        postfixesOfFilesToCheck.forEach {
            if (File("$filePathPrefix.${it.text}.txt").exists())
                Assert.fail("No $it, but file $filePathPrefix.${it.text}.txt exists.")
        }
    }

    fun runAndCompareWithExpected(checkUnexpectedBehaviour: ((Matcher?) -> Pair<Boolean, Set<Int>?>)? = null, runnable: () -> Unit) {
        try {
            runnable()
        } catch (e: TestsError) {
            val exceptionInfo = analyze(e.original)
            val unexpectedBehaviourCheckResult = checkUnexpectedBehaviour?.invoke(exceptionInfo)

            if (e is TestsCompilerError && unexpectedBehaviourCheckResult?.first == false)
                throw e.original

            val exceptionsFile = File("$filePathPrefix.${e.postfix.text}.txt")

            try {
                KotlinTestUtils.assertEqualsToFile(exceptionsFile, getExceptionInfo(e, unexpectedBehaviourCheckResult?.second))
            } catch (t: AssertionError) {
                e.original.printStackTrace()
                throw t
            }

            e.original.printStackTrace()
            validateExistingExceptionFiles(e)
            return
        }
        validateExistingExceptionFiles(null)
    }
}
