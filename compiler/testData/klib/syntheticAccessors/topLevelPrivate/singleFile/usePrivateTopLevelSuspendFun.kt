// WITH_COROUTINES
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

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