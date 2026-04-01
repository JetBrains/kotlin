// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.3
// ^^^ K/Wasm backend has issues KT-82803 & KT-83728 with FIR2IR v.2.3.0, fixed only in 2.4.0-Beta1. So, a test 2.3.0 frontend + current backend expectedly fails

import helpers.*
import kotlin.coroutines.*

suspend fun forContinue(i: Int): String? = if (i == 0) null else ""

suspend fun testContinue() {
    var i = 0
    var hit = 0
    while (i < 3) {
        val q = forContinue(i) ?: if (i == 0) {
            i++
            continue
        } else {
            error("unreachable")
        }
        if (q != "") error("fail")
        hit++
        i++
    }
    if (hit != 2) error("wrong: $hit")
}

suspend fun forBreak(): String? = null

suspend fun testBreakWithNestedBlock() {
    var i = 0
    var seen = false
    while (i < 3) {
        val q = forBreak() ?: if (i == 0) {
            val t = 42
            if (t != 42) error("fail")
            seen = true
            break
        } else {
            error("unreachable")
        }
        if (q != null) error("fail")
        i++
    }
    if (!seen) error("block not executed")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { testContinue() }
    builder { testBreakWithNestedBlock() }
    return "OK"
}
