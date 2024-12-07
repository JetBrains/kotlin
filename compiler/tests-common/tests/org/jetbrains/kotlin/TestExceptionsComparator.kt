/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

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

        private const val BYTECODE_ADDRESS = """\d{7}"""
        private val bytecodeAddressRegex = Regex(BYTECODE_ADDRESS)
        private val bytecodeAddressListRegex = Regex("""Bytecode:\s+($BYTECODE_ADDRESS:\s*([0-9a-f]{4}\s+)+\s+)+""")

        private fun unifyPlatformDependentOfException(exceptionText: String) =
            exceptionText.replace(bytecodeAddressListRegex) { bytecodeAddresses ->
                bytecodeAddresses.value.replace(bytecodeAddressRegex) { "0x${it.value}" }
            }
    }

    private val filePathPrefix = "${wholeFile.parent}/${wholeFile.nameWithoutExtension}.$EXCEPTIONS_FILE_PREFIX"

    private fun analyze(e: Throwable): Matcher? {
        for ((_, pattern) in exceptionMessagePatterns) {
            val matches = pattern.matcher(e.message ?: continue)
            if (matches.find()) return matches
        }

        return null
    }

    private fun getExceptionInfo(e: TestsError, exceptionByCases: Set<Int>?): String {
        val casesAsString = exceptionByCases?.run { "CASES: " + joinToString() + ls } ?: ""

        return when (e) {
            is TestsRuntimeError ->
                (e.original.cause ?: e.original).run {
                    val exceptionText = casesAsString + toString() + stackTrace[0]?.let { ls + it }
                    unifyPlatformDependentOfException(exceptionText)
                }
            is TestsCompilerError, is TestsCompiletimeError, is TestsInfrastructureError -> casesAsString + (e.original.cause ?: e.original).toString()
        }
    }

    private fun validateExistingExceptionFiles(e: TestsError?) {
        val postfixesOfFilesToCheck = TestsExceptionType.entries.toMutableSet().filter { it != e?.type }

        postfixesOfFilesToCheck.forEach {
            if (File("$filePathPrefix.${it.postfix}.txt").exists())
                Assert.fail("No $it, but file $filePathPrefix.${it.postfix}.txt exists.")
        }
    }

    fun run(expectedException: TestsExceptionType?, printExceptionsToConsole: Boolean = false, runnable: () -> Unit) {
        run(
            expectedException,
            mapOf(),
            computeExceptionPoint = null,
            printExceptionsToConsole,
            runnable
        )
    }

    fun run(
        expectedException: TestsExceptionType?,
        exceptionByCases: Map<Int, TestsExceptionType?>,
        computeExceptionPoint: ((Matcher?) -> Set<Int>?)?,
        printExceptionsToConsole: Boolean = false,
        runnable: () -> Unit
    ) {
        try {
            runnable()
        } catch (e: TestsError) {
            val analyzeResult = analyze(e.original)
            val casesWithExpectedException =
                computeExceptionPoint?.invoke(analyzeResult)?.filter { exceptionByCases[it] == e.type }?.toSet()

            if (casesWithExpectedException == null && e.type != expectedException) {
                throw e
            }

            val exceptionsFile = File("$filePathPrefix.${e.type.postfix}.txt")

            try {
                KotlinTestUtils.assertEqualsToFile(exceptionsFile, getExceptionInfo(e, casesWithExpectedException))
            } catch (t: AssertionError) {
                e.original.printStackTrace()
                throw t
            }
            if (printExceptionsToConsole) {
                e.original.printStackTrace()
            }
            validateExistingExceptionFiles(e)
            return
        }
        validateExistingExceptionFiles(null)
    }
}
