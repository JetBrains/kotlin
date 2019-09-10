// !LANGUAGE: +NewInference +VariadicGenerics
// TARGET_BACKEND: JVM
// WITH_COROUTINES
// WITH_RUNTIME

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runBlocking(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            (block as Function1<Continuation<Unit>, Any?>)(this)
        }
    })
}

class EmptyContinuation<T> : Continuation<T> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {}
}

suspend fun <vararg Ts, R> foo(
    vararg args: *Ts,
    block: suspend (*Ts) -> R
) {
    block(args)
    block.startCoroutineUninterceptedOrReturn(args, EmptyContinuation())
}

suspend fun <vararg Ts, R> bar (
    vararg args: *Ts,
    block: suspend (*Ts) -> R
) = foo(*args, block = block)

fun box(): String {
    var result: String = ""
    var intResult: Int = 0
    runBlocking {
        bar("O", 41, Unit, "K") { o, int, _, k ->
            intResult = int.inc()
            result = o + k
        }
    }
    if (intResult != 42)
        return "Int increment failure"
    return result
}
