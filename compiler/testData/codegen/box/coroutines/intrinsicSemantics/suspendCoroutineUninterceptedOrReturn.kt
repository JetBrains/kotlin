// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.test.assertEquals

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

suspend fun suspendWithException(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resumeWithException(RuntimeException("OK"))
    COROUTINE_SUSPENDED
}

fun builder(shouldSuspend: Boolean, c: suspend () -> String): String {
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

    if (counter != 0) throw RuntimeException("fail 0 $counter")

    if (shouldSuspend) {
        if (result !== COROUTINE_SUSPENDED) throw RuntimeException("fail 1")
        if (fromSuspension == null) throw RuntimeException("fail 2")
        return fromSuspension!!
    }

    if (result === COROUTINE_SUSPENDED) throw RuntimeException("fail 3")
    return result as String
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
    if (builder(false) { "OK" } != "OK") return "fail 4"
    if (builder(true) { suspendHere() } != "OK") return "fail 5"

    if (builder(false) { throw RuntimeException("OK") } != "Exception: OK") return "fail 6"
    if (builder(true) { suspendWithException() } != "Exception: OK") return "fail 7"

    return "OK"
}
