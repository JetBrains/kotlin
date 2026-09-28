// WITH_STDLIB
// WITH_COROUTINES

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
