// IGNORE_BACKEND: JS, JS_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt
// COMMON_COROUTINES_TEST

inline suspend fun foo(x: suspend () -> String) = x()

// FILE: box.kt
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: helpers.ContinuationAdapter<Unit>() {
        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

suspend fun String.id() = this

fun box(): String {
    var res = ""
    builder {
        res = foo("OK"::id)
    }
    return res
}
