package codegen.coroutines.degenerate2

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: SuccessOrFailure<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    println("s1")
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

suspend fun s2() {
    println("s2")
    s1()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Test fun runTest() {
    builder {
        s2()
    }
}