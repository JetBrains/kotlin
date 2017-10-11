package codegen.coroutines.withReceiver

import kotlin.test.*

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

class Controller {
    suspend fun suspendHere(): Int = suspendCoroutineOrReturn { x ->
        x.resume(42)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

@Test fun runTest() {
    var result = 0

    builder {
        result = suspendHere()
    }

    println(result)
}