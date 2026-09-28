// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

suspend fun f(i: Int): String? = if (i == 1) "OK" else null

suspend fun test(): String {
    var i = 0
    do {
        val q = f(i) ?:
        if (i == 0) {
            i++
            continue
        } else {
            error("unreachable")
        }
        return q
    } while (i < 2)
    return "FAIL"
}

fun builder(c: suspend () -> String): String {
    var result = "FAIL"
    c.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

fun box(): String = builder { test() }
