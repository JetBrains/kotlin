// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.3
// ^^^ K/Wasm backend has issues KT-82803 & KT-83728 with FIR2IR v.2.3.0, fixed only in 2.4.0-Beta1. So, a test 2.3.0 frontend + current backend expectedly fails

import helpers.*
import kotlin.coroutines.*

suspend fun forBreak(x: Int): String? = if (x < 2) "" else null

suspend fun testBreak() {
    var count = 0
    for (i in 0..4) {
        val q = forBreak(i) ?: when {
            i == 2 -> break
            else -> error("unexpected")
        }
        if (q != "") error("fail")
        count++
    }
    if (count != 2) error("count=$count")
}

suspend fun forContinue(x: Int): String? = if (x == 1) null else "v$x"

suspend fun testContinue() {
    var acc = ""
    for (i in 0..2) {
        val q = forContinue(i) ?: if (i == 1) {
            continue
        } else {
            error("unexpected")
        }
        acc += q
    }
    if (acc != "v0v2") error("bad acc: $acc")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { testBreak() }
    builder { testContinue() }
    return "OK"
}
