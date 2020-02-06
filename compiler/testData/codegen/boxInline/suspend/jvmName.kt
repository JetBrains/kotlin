// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM

import COROUTINES_PACKAGE.*
import helpers.*

class Result<T>(val x: T)

@JvmName("foo") // + foo$$forInline
suspend inline fun test(c: Result<Int>) = c.x

@JvmName("bar") // + bar$$forInline
suspend inline fun test(c: Result<String>) = c.x

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var res = "FAIL"
    builder {
        res = test(Result("OK"))
    }
    return res
}
