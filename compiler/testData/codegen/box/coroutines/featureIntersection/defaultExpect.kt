// !LANGUAGE: +MultiPlatformProjects
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.coroutines.*

var res = 0L

expect suspend fun withLimit(limit: Long = 42L)

actual suspend fun withLimit(limit: Long) {
    res = limit
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {})
}

fun box(): String {
    builder {
        withLimit()
    }
    return if (res == 42L) "OK" else "FAIL $res"
}
