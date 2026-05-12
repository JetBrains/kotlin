// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2,2.3
// ^^^ KT-82803, KT-83728 are fixed in 2.4.0-Beta1

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
