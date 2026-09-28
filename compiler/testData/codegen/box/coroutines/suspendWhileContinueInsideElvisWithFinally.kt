// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

suspend fun f(i: Int): String? = if (i == 1) "OK" else null

suspend fun test(): String {
    var i = 0
    var finallyCount = 0

    while (i < 2) {
        try {
            val q = f(i) ?:
            if (i == 0) {
                i++
                continue
            } else {
                error("unreachable")
            }
            return if (finallyCount == 1) q else "FAIL: finallyCount=$finallyCount"
        } finally {
            finallyCount++
        }
    }

    return "FAIL"
}

fun builder(c: suspend () -> String): String {
    var result = "FAIL"
    c.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

fun box(): String = builder { test() }
