// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import kotlin.test.assertEquals

suspend fun suspendHereUnintercepted(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

suspend fun suspendWithExceptionUnintercepted(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resumeWithException(RuntimeException("OK"))
    COROUTINE_SUSPENDED
}

suspend fun suspendHereIntercepted(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.intercepted().resume("OK")
    COROUTINE_SUSPENDED
}

suspend fun suspendWithExceptionIntercepted(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.intercepted().resumeWithException(RuntimeException("OK"))
    COROUTINE_SUSPENDED
}

fun builder(expectedCount: Int, c: suspend () -> String): String {
    var fromSuspension: String? = null
    var counter = 0

    val result = try {
        c.startCoroutineUninterceptedOrReturn(object: ContinuationAdapter<String>() {
            override val context: CoroutineContext
                get() =  ContinuationDispatcher { counter++ }

            override fun resumeWithException(exception: Throwable) {
                fromSuspension = "Exception: " + exception.message!!
            }

            override fun resume(value: String) {
                fromSuspension = value
            }
        })
    } catch (e: Exception) {
        "Exception: ${e.message}"
    }

    if (counter != expectedCount) throw RuntimeException("fail 0")
    return fromSuspension!!
}

class ContinuationDispatcher(val dispatcher: () -> Unit) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(dispatcher, continuation)
}

private class DispatchedContinuation<T>(
        val dispatcher: () -> Unit,
        val continuation: Continuation<T>
): ContinuationAdapter<T>() {
    override val context: CoroutineContext = continuation.context

    override fun resume(value: T) {
        dispatcher()
        continuation.resume(value)
    }

    override fun resumeWithException(exception: Throwable) {
        dispatcher()
        continuation.resumeWithException(exception)
    }
}

fun box(): String {
    if (builder(0) { suspendHereUnintercepted() } != "OK") return "fail 2"
    if (builder(1) { suspendHereIntercepted() } != "OK") return "fail 3"

    if (builder(0) { suspendWithExceptionUnintercepted() } != "Exception: OK") return "fail 4"
    if (builder(1) { suspendWithExceptionIntercepted() } != "Exception: OK") return "fail 5"

    return "OK"
}
