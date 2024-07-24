/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

private val SKIP_MUTED_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.skip.muted.tests")

fun isMutedInDatabase(testClass: Class<*>, methodKey: String): Boolean {
    val mutedTest = mutedSet.mutedTest(testClass, methodKey)
    return SKIP_MUTED_TESTS && isPresentedInDatabaseWithoutFailMarker(mutedTest)
}

fun isMutedInDatabaseWithLog(testClass: Class<*>, methodKey: String): Boolean {
    val mutedInDatabase = isMutedInDatabase(testClass, methodKey)

    if (mutedInDatabase) {
        System.err.println(mutedMessage(testClass, methodKey))
    }

    return mutedInDatabase
}

fun isPresentedInDatabaseWithoutFailMarker(mutedTest: MutedTest?): Boolean {
    return mutedTest != null && !mutedTest.hasFailFile
}

fun mutedMessage(klass: Class<*>, methodKey: String): String = "MUTED TEST: ${testKey(klass, methodKey)}"

fun testKey(klass: Class<*>, methodKey: String): String = "${klass.canonicalName}.$methodKey"

fun wrapWithMuteInDatabase(testClass: Class<*>, methodName: String, f: () -> Unit): (() -> Unit)? {
    val mutedTest = getMutedTest(testClass, methodName)
    val testKey = testKey(testClass, methodName)

    if (isMutedInDatabase(testClass, methodName)) {
        return {
            System.err.println(mutedMessage(testClass, methodName))
        }
    } else if (isPresentedInDatabaseWithoutFailMarker(mutedTest)) {
        if (mutedTest?.isFlaky == true) {
            return f
        } else {
            return {
                invertMutedTestResultWithLog(f, testKey)
            }
        }
    } else {
        return wrapWithAutoMute(f, testKey)
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
