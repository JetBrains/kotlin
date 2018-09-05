// !LANGUAGE: +NewInference
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, JS_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt

inline suspend fun go(f: () -> String) = f()

// FILE: box.kt
// WITH_RUNTIME

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: SuccessOrFailure<Unit>) {
            result.getOrThrow()
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
