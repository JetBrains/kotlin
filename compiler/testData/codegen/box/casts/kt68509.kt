// WITH_COROUTINES
// WITH_STDLIB

import kotlin.coroutines.*

open class MyContinuation : Continuation<Any?> {
    var coroutineResult: Any? = null
    override val context: CoroutineContext = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) { coroutineResult = result.getOrNull() }
}

inline fun <R> inlineFun(some: R, block: (R) -> R): R  {
    return block(some ?: return some)
}

suspend fun simpleSuspend(e: String): String = e

suspend fun suspendFun(e: String): String {
    return inlineFun(e) { simpleSuspend(it) }
}

fun box(): String {
    val continuation = MyContinuation()
    (::suspendFun).startCoroutine("OK", continuation)
    return (continuation.coroutineResult as? String) ?: "Fail"
}