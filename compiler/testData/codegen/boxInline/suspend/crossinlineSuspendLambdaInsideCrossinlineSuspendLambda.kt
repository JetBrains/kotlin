// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
import helpers.*

inline suspend fun foo(crossinline a: suspend () -> Unit, crossinline b: suspend () -> Unit) {
    var x = "OK"
    bar { x; a(); b() }
}

inline suspend fun bar(crossinline l: suspend () -> Unit) {
    val c : suspend () -> Unit = { l() }
    c()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

fun box(): String {
    var y = "fail"
    builder {
        foo({ y = "O" }) { y += "K" }
    }
    return y
}