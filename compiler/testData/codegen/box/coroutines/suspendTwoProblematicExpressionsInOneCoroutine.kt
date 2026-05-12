// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2,2.3
// ^^^ KT-82803, KT-83728 are fixed in 2.4.0-Beta1

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
