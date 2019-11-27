/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.jetbrains.kotlin.test.KotlinTestUtils.DoTest
import org.junit.Assert
import java.io.File

private val RUN_MUTED_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.run.muted.tests")
private val AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT: String? = null
private val AUTOMATICALLY_GENERATE_FAIL_FILE_FOR_FAILED_TESTS: Boolean = false

annotation class MuteExtraSuffix(val value: String = "")

@Throws(Exception::class)
fun testWithMuteInFile(test: DoTest, testCase: TestCase): DoTest {
    val extraSuffix = testCase.javaClass.getAnnotation(MuteExtraSuffix::class.java)?.value ?: ""
    return testWithMuteInFile(test, extraSuffix)
}

@Throws(Exception::class)
fun testWithMuteInFile(test: DoTest, extraSuffix: String): DoTest {
    return object : DoTest {
        override fun invoke(filePath: String) {
            val testDataFile = File(filePath)

            val isMutedWithFile = isMutedWithFile(testDataFile, extraSuffix)
            if (isMutedWithFile && !RUN_MUTED_TESTS) {
                System.err.println("MUTED TEST: $filePath")
                return
            }

            val failFile = failFile(testDataFile, extraSuffix)
            val hasFailFile = failFile != null

            try {
                test.invoke(filePath)
            } catch (e: Throwable) {
                if (checkFailFile(e, testDataFile, extraSuffix)) {
                    System.err.println("MUTED TEST (FAIL): $filePath")
                    return
                }

                if (!isMutedWithFile && !hasFailFile && AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT != null) {
                    createMuteFile(testDataFile, extraSuffix, AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT)
                }
                throw e
            }

            Assert.assertNull("Test is good but there is a fail file", failFile)
        }
    }
}

private fun isMutedWithFile(testPathFile: File, extraSuffix: String): Boolean {
    return muteFile(testPathFile, extraSuffix) != null
}

private fun createMuteFile(testDataFile: File, extraSuffix: String, text: String) {
    require(text.isNotEmpty()) { "Mute text must not be empty" }

    muteFileNoCheck(testDataFile, extraSuffix).writeText(text)
}

private fun rawFileNoChecks(testPathFile: File, extraSuffix: String, suffix: String): File {
    return when {
        testPathFile.isDirectory ->
            File(testPathFile, "${testPathFile.name}$extraSuffix$suffix")
        else ->
            File("${testPathFile.path}$extraSuffix$suffix")
    }
}

private fun muteFileNoCheck(testPathFile: File, extraSuffix: String): File {
    return rawFileNoChecks(testPathFile, extraSuffix, ".mute")
}

private fun failFileNoCheck(testPathFile: File, extraSuffix: String): File {
    return rawFileNoChecks(testPathFile, extraSuffix, ".fail")
}

private fun muteFile(testPathFile: File, extraSuffix: String): File? {
    val muteFile = muteFileNoCheck(testPathFile, extraSuffix)

    if (muteFile.exists() && muteFile.isFile) {
        return muteFile
    }

    return null
}

private fun failFile(testDataFile: File, extraSuffix: String): File? {
    val failFile = failFileNoCheck(testDataFile, extraSuffix)

    if (!failFile.exists() || !failFile.isFile) {
        return null
    }

    return failFile
}

private fun failMessage(failure: Throwable): String {
    val cause = failure.cause
    return failure.message +
            if (cause != null) {
                "\n" + cause
            } else {
                ""
            }
}

private fun checkFailFile(failure: Throwable, testDataFile: File, extraSuffix: String): Boolean {
    val failFile = failFile(testDataFile, extraSuffix)

    if (failFile == null) {
        if (AUTOMATICALLY_GENERATE_FAIL_FILE_FOR_FAILED_TESTS) {
            failFileNoCheck(testDataFile, extraSuffix).writeText(failMessage(failure))
        }

        return false
    }

    KotlinTestUtils.assertEqualsToFile(failFile, failMessage(failure))
    return true
}