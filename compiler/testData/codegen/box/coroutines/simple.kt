// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

//public suspend inline fun <T> suspendCoroutineOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
//    suspendCoroutineUninterceptedOrReturn { cont -> block(cont.intercepted()) }
//
//public inline fun <T> Continuation<T>.resume(value: T): Unit =
//    resumeWith(SuccessOrFailure.success(value))

suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
