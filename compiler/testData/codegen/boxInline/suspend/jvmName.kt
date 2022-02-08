// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: test.kt
// TARGET_BACKEND: JVM

import kotlin.coroutines.*
import helpers.*

class Result<T>(val x: T)

@JvmName("foo") // + foo$$forInline
suspend inline fun test(c: Result<Int>) = c.x

@JvmName("bar") // + bar$$forInline
suspend inline fun test(c: Result<String>) = c.x

// FILE: box.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
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
