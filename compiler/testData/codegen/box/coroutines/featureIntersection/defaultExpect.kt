// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE, WASM
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)

import kotlin.coroutines.*

var res = 0L

expect suspend fun withLimit(limit: Long = 42L)

actual suspend fun withLimit(limit: Long) {
    res = limit
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        withLimit()
    }
    return if (res == 42L) "OK" else "FAIL $res"
}
