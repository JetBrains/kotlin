// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

inline suspend fun foo(x: suspend () -> String) = x()

suspend fun String.id() = this

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = ""
    builder {
        res = foo("OK"::id)
    }
    return res
}
