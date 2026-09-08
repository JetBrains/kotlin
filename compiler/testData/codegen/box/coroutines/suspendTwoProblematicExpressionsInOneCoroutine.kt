// WITH_STDLIB
// WITH_COROUTINES

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
