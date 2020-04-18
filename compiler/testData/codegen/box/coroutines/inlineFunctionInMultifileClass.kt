// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// IGNORE_LIGHT_ANALYSIS
// !INHERIT_MULTIFILE_PARTS
// TARGET_BACKEND: JVM

// FILE: test.kt

@file:JvmMultifileClass
@file:JvmName("Test")

package test

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun foo(): String = bar("OK")

suspend inline fun bar(result: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(result)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun test(): String {
    var result = ""

    builder {
        result = foo()
    }

    return result
}

// FILE: box.kt

fun box(): String = test.test()
