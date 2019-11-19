// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// TARGET_BACKEND: JVM

// IGNORE_BACKEND: JVM_IR
// When -Xmultifile-parts-inherit is disabled, JVM IR backend generates "bridges" that delegate into part members and puts them into
// the multifile facade. But since the multifile facade phase happens after coroutines, continuations are not created for suspend functions.

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
