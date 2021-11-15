// WITH_STDLIB

import kotlin.coroutines.*

fun launch(block: suspend (Long) -> String): String {
    var result = ""
    block.startCoroutine(0L, Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

suspend fun g() {}

class C {
    suspend fun f(i: Long): String {
        var x = 0
        listOf<Int>().map { x }
        g()
        return "OK"
    }
}

fun box(): String = launch(C()::f)