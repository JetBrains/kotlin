package codegen.coroutines.coroutineContext2

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: SuccessOrFailure<Any?>) { result.getOrThrow() }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo() = coroutineContext

@Test fun runTest() {
    builder { println(foo()) }
}