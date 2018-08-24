package codegen.coroutines.returnsUnit1

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

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline suspend fun inline_s2(x: Int): Unit {
    println(x)
    s1()
}

suspend fun s3(x: Int) {
    inline_s2(x)
}

@Test fun runTest() {
    var result = 0

    builder {
        s3(117)
    }

    println(result)
}