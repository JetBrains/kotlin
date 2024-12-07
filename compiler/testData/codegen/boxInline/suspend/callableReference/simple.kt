// WITH_COROUTINES
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: test.kt

inline suspend fun foo(x: suspend () -> String) = x()

// FILE: box.kt

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext get() = EmptyCoroutineContext
        override fun resumeWith(value: Result<Unit>) {
            value.getOrThrow()
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
