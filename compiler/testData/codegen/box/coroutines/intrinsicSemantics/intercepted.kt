// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
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

fun builder(testNum: Int, expectedCount: Int, c: suspend () -> String): String {
    var fromSuspension: String? = null
    var counter = 0

    c.startCoroutineUninterceptedOrReturn(object : Continuation<String> {
        override val context: CoroutineContext
            get() = ContinuationDispatcher { counter++ }

        override fun resumeWith(value: Result<String>) {
            fromSuspension = try {
                value.getOrThrow()
            } catch (exception: Throwable) {
                "Exception: " + exception.message!!
            }
        }
    })

    if (counter != expectedCount) throw RuntimeException("fail 0 $testNum $counter != $expectedCount")
    return fromSuspension!!
}

class ContinuationDispatcher(val dispatcher: () -> Unit) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(dispatcher, continuation)
}

private class DispatchedContinuation<T>(
        val dispatcher: () -> Unit,
        val continuation: Continuation<T>
): Continuation<T> {
    override val context: CoroutineContext = continuation.context

    override fun resumeWith(value: Result<T>) {
        dispatcher()
        continuation.resumeWith(value)
    }
}

fun box(): String {
    if (builder(0, 0) { suspendHereUnintercepted() } != "OK") return "fail 2"
    if (builder(1, 1) { suspendHereIntercepted() } != "OK") return "fail 3"

    if (builder(2, 0) { suspendWithExceptionUnintercepted() } != "Exception: OK") return "fail 4"
    if (builder(3, 1) { suspendWithExceptionIntercepted() } != "Exception: OK") return "fail 5"

    if (builder(4, 0, ::suspendHereUnintercepted) != "OK") return "fail 6"
    if (builder(5, 1, ::suspendHereIntercepted) != "OK") return "fail 7"

    if (builder(6, 0, ::suspendWithExceptionUnintercepted) != "Exception: OK") return "fail 8"
    if (builder(7, 1, ::suspendWithExceptionIntercepted) != "Exception: OK") return "fail 9"

    if (builder(8, 0) {
            suspend {}()
            suspendHereUnintercepted()
    } != "OK") return "fail 21"
    if (builder(9, 1) {
            suspend {}()
            suspendHereIntercepted()
    } != "OK") return "fail 31"

    if (builder(10, 0) {
            suspend {}()
            suspendWithExceptionUnintercepted()
    } != "Exception: OK") return "fail 41"
    if (builder(11, 1) {
            suspend {}()
            suspendWithExceptionIntercepted()
    } != "Exception: OK") return "fail 51"

    return "OK"
}
