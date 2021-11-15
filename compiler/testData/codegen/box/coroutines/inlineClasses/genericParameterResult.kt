// WITH_STDLIB

import kotlin.coroutines.*

var stdout = ""
fun println(s: Any?) { s?.let { stdout += it } }

fun box(): String {
    suspend {
        listOf(Result.success(true)).forEach {
            println(it.getOrNull())
        }
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })

    if (stdout != "true") return "FAIL: forEach lambda inside a coroutine wasn't called"

    return "OK"
}