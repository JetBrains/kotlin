// IGNORE_BACKEND: JS_IR
// The test if failing for JS on IR visibility validation but for
// invisible `internal fun sharedBoxCreate` from stdlib.
// See https://youtrack.jetbrains.com/issue/KT-67304
// WITH_COROUTINES

import kotlin.coroutines.*

private suspend fun privateSuspendMethod() = "OK"

internal suspend inline fun internalInline() = privateSuspendMethod()

fun runBlocking(c: suspend () -> String): String {
    var res: String? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

fun box(): String = runBlocking {
    internalInline()
}