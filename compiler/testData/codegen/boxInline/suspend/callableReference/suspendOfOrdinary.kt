// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, JS_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt

inline suspend fun go(f: () -> String) = f()

// FILE: box.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

suspend fun String.id(): String = this

fun box(): String {
    val x = "OK"
    var res = "FAIL"
    builder {
        res = go(x::id)
    }
    return res
}
