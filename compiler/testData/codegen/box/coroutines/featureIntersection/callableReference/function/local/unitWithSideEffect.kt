// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var state = 23

fun box(): String {
    suspend fun incrementState(inc: Int) {
        state += inc
    }

    val inc = ::incrementState
    builder {
        inc(12)
        inc(-5)
        inc(27)
        inc(-15)
    }

    return if (state == 42) "OK" else "Fail $state"
}
