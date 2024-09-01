// IGNORE_BACKEND: JS_IR
// ^^^ This test fails due to visibility violation on access to JS `internal` intrinsic functions
//     `kotlin.sharedBoxCreate`, `kotlin.sharedBoxRead` and `kotlin.sharedBoxWrite`. To be fixed in KT-67304.
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