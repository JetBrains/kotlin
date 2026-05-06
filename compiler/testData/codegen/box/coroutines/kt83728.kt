// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2,2.3
// ^^^ KT-82803, KT-83728 are fixed in 2.4.0-Beta1

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
