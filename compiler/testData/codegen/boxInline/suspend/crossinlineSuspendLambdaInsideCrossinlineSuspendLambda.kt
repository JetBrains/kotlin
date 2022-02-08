// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt

import kotlin.coroutines.*
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
fun box(): String {
    var y = "fail"
    builder {
        foo({ y = "O" }) { y += "K" }
    }
    return y
}
