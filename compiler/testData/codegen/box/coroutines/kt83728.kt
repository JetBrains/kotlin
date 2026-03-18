// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.3
// ^^^ K/Wasm backend has issues KT-82803 & KT-83728 with FIR2IR v.2.3.0, fixed only in 2.4.0-Beta1. So, a test `2.3.0 frontend + current backend` expectedly fails

import helpers.*
import kotlin.coroutines.*

suspend fun f(): String? = ""

suspend fun x() {
    var i = 0
    while (i < 2) {
        val q = f() ?:
            if (2.hashCode() == 1234) {
                break
            } else {
                error("")
            }
        i++
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        x()
    }
    return "OK"
}
