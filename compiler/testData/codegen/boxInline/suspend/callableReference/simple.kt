// !LANGUAGE: +ReleaseCoroutines
// !API_VERSION: 1.3
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt

inline suspend fun foo(x: suspend () -> String) = x()

// FILE: box.kt
// WITH_RUNTIME
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
