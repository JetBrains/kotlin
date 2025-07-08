/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

fun isMutedInDatabaseWithLog(testClass: Class<*>, methodKey: String): Boolean {
    val mutedInDatabase = getMutedTest(testClass, methodKey) != null

    if (mutedInDatabase) {
        System.err.println(mutedMessage(testClass, methodKey))
    }

    return mutedInDatabase
}

fun mutedMessage(klass: Class<*>, methodKey: String): String = "MUTED TEST: ${testKey(klass, methodKey)}"

fun testKey(klass: Class<*>, methodKey: String): String = "${klass.canonicalName}.$methodKey"

fun wrapWithMuteInDatabase(testClass: Class<*>, methodName: String, f: () -> Unit): (() -> Unit) {
    val mutedTest = getMutedTest(testClass, methodName)
    val testKey = testKey(testClass, methodName)

    if (mutedTest != null) {
        if (mutedTest.isFlaky) {
            return {
                System.err.println(mutedMessage(testClass, methodName))
            }
        } else {
            return {
                invertMutedTestResultWithLog(f, testKey)
            }
        }
    } else {
        return {
            try {
                f()
            } catch (e: Throwable) {
                System.err.println(
                    """FAILED TEST: if it's a cross-push add to mute-common.csv:
                    ${testClass.canonicalName}.$methodName,KT-XXXX,STABLE
                    or if you consider it's flaky:
                    ${testClass.canonicalName}.$methodName,KT-XXXX,FLAKY""".trimIndent()
                )
                throw e
            }
        }
    }
}

fun invertMutedTestResultWithLog(f: () -> Unit, testKey: String) {
    var isTestGreen = true
    try {
        f()
    } catch (_: Throwable) {
        println("MUTED TEST STILL FAILS: $testKey")
        isTestGreen = false
    }

    if (isTestGreen) {
        System.err.println("SUCCESS RESULT OF MUTED TEST: $testKey")
        throw Exception("Muted non-flaky test $testKey finished successfully. Please remove it from csv file")
    }
}
