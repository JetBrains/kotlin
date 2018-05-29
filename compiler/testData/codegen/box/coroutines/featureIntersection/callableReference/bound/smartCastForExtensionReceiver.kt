// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class B

suspend fun B.magic() {
}

suspend fun suspendRun(c: suspend() -> Unit) = c()

fun boom(a: Any) {
    when (a) {
        is B -> builder { suspendRun(a::magic) }
    }
}

fun box(): String {
    boom(B())
    return "OK"
}
