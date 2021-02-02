// WITH_RUNTIME
// IGNORE_BACKEND: JS_IR

import kotlin.coroutines.*

fun box(): String {
    suspend {
        listOf(Result.success(true)).forEach {
            println(it.getOrNull())
        }
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return "OK"
}