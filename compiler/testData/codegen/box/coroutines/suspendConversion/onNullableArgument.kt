// WITH_STDLIB
// WITH_COROUTINES
// ISSUE: KT-79562
// IGNORE_BACKEND_K1: ANY
// K1 reports TYPE_MISMATCH

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0 2.2.0
// ^^^ KT-79562 fixed in 2.3.0-Beta1

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
