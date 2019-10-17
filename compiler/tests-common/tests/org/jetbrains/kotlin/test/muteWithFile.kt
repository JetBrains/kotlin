/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.KotlinTestUtils.DoTest
import org.junit.Assert
import java.io.File

private val RUN_MUTED_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.run.muted.tests")
private val AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT: String? = null

@Throws(Exception::class)
fun testWithMuteInFile(test: DoTest): DoTest {
    return object : DoTest {
        override fun invoke(filePath: String) {
            val testDataFile = File(filePath)

            val isMutedWithFile = isMutedWithFile(testDataFile)
            if (isMutedWithFile && !RUN_MUTED_TESTS) {
                System.err.println("IGNORED TEST: $filePath")
                return
            }

            val failFile = failFile(testDataFile)
            val hasFailFile = failFile != null

            try {
                test.invoke(filePath)
            } catch (e: Throwable) {
                if (checkFailFile(e, testDataFile)) {
                    return
                }

                if (!isMutedWithFile && !hasFailFile && AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT != null) {
                    createMuteFile(testDataFile, AUTOMATICALLY_MUTE_FAILED_TESTS_WITH_CONTENT)
                }
                throw e
            }

            Assert.assertNull("Test is good but there is a fail file", failFile)
        }
    }
}

private fun isMutedWithFile(testDataFile: File): Boolean {
    if (!testDataFile.isFile) {
        return false
    }
    val muteFile = File("${testDataFile.path}.mute")
    return muteFile.exists() && muteFile.isFile
}

private fun createMuteFile(testDataFile: File, text: String) {
    require(text.isNotEmpty()) { "Mute text must not be empty" }

    File("${testDataFile.path}.mute").writeText(text)
}

private fun failFile(testDataFile: File): File? {
    if (!testDataFile.isFile) return null

    val failFile = File("${testDataFile.path}.fail")
    if (!failFile.exists() || !failFile.isFile) {
        return null
    }

    return failFile
}

private fun checkFailFile(failure: Throwable, testDataFile: File): Boolean {
    val failFile = failFile(testDataFile) ?: return false
    val cause = failure.cause
    val muteMessage = failure.message +
            if (cause != null) {
                "\n" + cause
            } else {
                ""
            }

    KotlinTestUtils.assertEqualsToFile(failFile, muteMessage)

    return true
}