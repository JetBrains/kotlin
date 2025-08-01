// WITH_STDLIB
// WITH_COROUTINES
// ISSUE: KT-79562
// IGNORE_BACKEND_K2: ANY
// IGNORE_BACKEND_K1: ANY
// K1 reports TYPE_MISMATCH

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun runS(fn: (suspend () -> Unit)?) = fn?.invoke()

val lambda: (() -> Unit)? = null

fun box(): String {
    var test = "OK"
    builder {
        runS(lambda)
    }
    return "OK"
}
