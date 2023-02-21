// WITH_STDLIB

import kotlin.coroutines.*

suspend fun test() {
    Nexus.initialize1()
    while (true) { }
}

object Nexus {
    suspend fun initialize1() {
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        suspendCoroutine<Unit> {}
        test()
    }
    return "OK"
}