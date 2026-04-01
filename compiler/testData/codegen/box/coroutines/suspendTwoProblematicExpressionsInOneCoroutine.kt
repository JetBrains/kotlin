// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.3
// ^^^ K/Wasm backend has issues KT-82803 & KT-83728 with FIR2IR v.2.3.0, fixed only in 2.4.0-Beta1. So, a test 2.3.0 frontend + current backend expectedly fails

import helpers.*
import kotlin.coroutines.*

suspend fun f1(): String? = ""
suspend fun f2(): String? = null

suspend fun test() {
    val a = f1() ?: error("unexpected")
    if (a != "") error("fail")

    var hit = false
    while (true) {
        val b = f2() ?: if (!hit) {
            hit = true
            break
        } else {
            error("unreachable")
        }
        error("unexpected: $b")
    }

    val c = f1() ?: error("unexpected2")
    if (c != "") error("fail2")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { test() }
    return "OK"
}