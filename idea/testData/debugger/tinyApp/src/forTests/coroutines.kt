package forTests

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(data: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}
