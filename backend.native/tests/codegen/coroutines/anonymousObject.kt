package codegen.coroutines.anonymousObject

import kotlin.test.*

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun suspendHere(): Int = suspendCoroutineOrReturn { x ->
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface I {
    suspend fun foo(lambda: suspend (String) -> Unit)
    suspend fun bar(s: String)
}

fun create() = object: I {
    var lambda: suspend (String) -> Unit = {}

    override suspend fun foo(lambda: suspend (String) -> Unit) {
        this.lambda = lambda
    }

    override suspend fun bar(s: String) {
        lambda(s)
    }
}

@Test fun runTest() {
    builder {
        val z = create()
        z.foo { suspendHere(); println(it) }
        z.bar("zzz")
    }
}