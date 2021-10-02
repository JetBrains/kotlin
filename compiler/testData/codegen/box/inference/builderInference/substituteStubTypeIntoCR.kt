// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

suspend fun foo() {
    val x = sequence {
        yield(1)
        ::`yield`
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { foo() }
    return "OK"
}
