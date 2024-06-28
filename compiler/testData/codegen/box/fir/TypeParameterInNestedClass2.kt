// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-58008

import kotlin.coroutines.*

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

class Owner<C>(val c: C) {
    class Builder<B>(
        private val action: suspend () -> B,
    ) {
        fun foo() = runBlocking {
            action.invoke()
        }
    }

    fun <W> withExponentialBackoff(action: () -> W): Builder<W> {
        return Builder(action)
    }
}

fun box(): String {
    return Owner("").withExponentialBackoff { "OK" }.foo()
}
