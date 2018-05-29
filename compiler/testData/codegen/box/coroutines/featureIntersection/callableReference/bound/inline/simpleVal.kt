// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

inline suspend fun go(f: suspend () -> String) = f()

suspend fun String.id(): String = this

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val x = "OK"
    var res = "FAIL"
    builder {
        res = go(x::id)
    }
    return res
}
