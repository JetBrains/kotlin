// !LANGUAGE: +ReleaseCoroutines
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt

inline suspend fun foo(x: suspend () -> String) = x()

// FILE: box.kt
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.ContinuationAdapter
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: ContinuationAdapter<Unit>() {
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
