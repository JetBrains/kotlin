// WITH_STDLIB
// WITH_COROUTINES

// FILE: inline.kt

inline class IC(val s: String)

suspend fun o(): IC = IC("O")
suspend fun k(): IC = IC("K")

inline suspend fun inlineMe(): String {
    return o().s + k().s
}

// FILE: box.kt

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = inlineMe()
    }
    return res
}
