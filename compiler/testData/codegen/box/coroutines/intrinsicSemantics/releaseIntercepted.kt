// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var c: Continuation<String>? = null
var interceptions: Int = 0
var releases: Int = 0
var invocations: Int = 0

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    invocations++
    c = x
    COROUTINE_SUSPENDED
}

suspend fun suspendHereIntercepted(): String = suspendCoroutineUninterceptedOrReturn { x ->
    invocations++
    c = x.intercepted()
    COROUTINE_SUSPENDED
}

fun builder(testNum: Int, expectedResult: Result<String>, c: suspend () -> String) {
    c.startCoroutineUninterceptedOrReturn(object : Continuation<String> {
        override val context: CoroutineContext
            get() = ContinuationDispatcher()

        override fun resumeWith(result: Result<String>) {
            if (result != expectedResult) {
                error("fail $testNum: unexpected result: $expectedResult != $result")
            }
        }
    })
}

class ContinuationDispatcher : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(continuation)

    override fun releaseInterceptedContinuation(continuation: Continuation<*>) {
        releases++
    }
}

private class DispatchedContinuation<T>(
    val continuation: Continuation<T>
): Continuation<T> {
    init {
        interceptions++
    }

    override val context: CoroutineContext = continuation.context

    override fun resumeWith(result: Result<T>) {
        continuation.resumeWith(result)
    }
}

fun box(): String {
    val success = Result.success("OK")

    builder(0, success) { suspendHere() }
    if (invocations != 1) return "fail 00: $invocations"
    if (interceptions != 0) return "fail 01: $interceptions"
    if (releases != 0) return "fail 02: $releases"
    c = c?.intercepted()
    if (interceptions != 1) return "fail 03: $interceptions"
    c?.resumeWith(success)
    if (releases != 1) return "fail 04: $releases"

    builder(1, success) { suspendHereIntercepted() }
    if (invocations != 2) return "fail 10: $invocations"
    if (interceptions != 2) return "fail 11: $interceptions"
    if (releases != 1) return "fail 12: $interceptions"
    c?.resumeWith(success)
    if (interceptions != 2) return "fail 13: $interceptions"
    if (releases != 2) return "fail 14: $releases"

    builder(2, success, ::suspendHere)
    if (invocations != 3) return "fail 20: $invocations"
    if (interceptions != 2) return "fail 21: $interceptions"
    if (releases != 2) return "fail 22: $releases"
    c = c?.intercepted()
    if (interceptions != 3) return "fail 23: $interceptions"
    c?.resumeWith(success)
    if (releases != 3) return "fail 24: $releases"

    builder(3, success, ::suspendHereIntercepted)
    if (invocations != 4) return "fail 30: $invocations"
    if (interceptions != 4) return "fail 31: $interceptions"
    if (releases != 3) return "fail 32: $interceptions"
    c?.resumeWith(success)
    if (interceptions != 4) return "fail 33: $interceptions"
    if (releases != 4) return "fail 34: $releases"

    builder(4, success) { suspend {}(); suspendHere() }
    if (invocations != 5) return "fail 40: $invocations"
    if (interceptions != 4) return "fail 41: $interceptions"
    if (releases != 4) return "fail 42: $releases"
    c = c?.intercepted()
    if (interceptions != 5) return "fail 43: $interceptions"
    c?.resumeWith(success)
    if (releases != 5) return "fail 44: $releases"

    builder(5, success) { suspend {}(); suspendHereIntercepted() }
    if (invocations != 6) return "fail 50: $invocations"
    if (interceptions != 6) return "fail 51: $interceptions"
    if (releases != 5) return "fail 52: $interceptions"
    c?.resumeWith(success)
    if (interceptions != 6) return "fail 53: $interceptions"
    if (releases != 6) return "fail 54: $releases"

    return "OK"
}
