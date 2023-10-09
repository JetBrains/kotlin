// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-58008

// JVM_ABI_K1_K2_DIFF: KT-62793

import kotlin.coroutines.*

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

object Retry {
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
    return Retry.withExponentialBackoff { "OK" }.foo()
}
