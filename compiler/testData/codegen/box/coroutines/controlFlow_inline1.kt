// WITH_STDLIB
// WITH_COROUTINES

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

val sb = StringBuilder()

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s1")
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline suspend fun inline_s2(): Int {
    return 42
}

fun box(): String {
    var result = 0

    builder {
        result = inline_s2()
    }

    sb.appendLine(result)

    assertEquals("42\n", sb.toString())
    return "OK"
}
