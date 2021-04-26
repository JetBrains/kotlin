// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@Suppress("UNSUPPORTED_FEATURE")
inline class I(val x: Any?)

suspend fun <T> suspendHere(): T = suspendCoroutineUninterceptedOrReturn {
    c = it as Continuation<Any?>
    COROUTINE_SUSPENDED
}

var c: Continuation<Any?>? = null

class C {
    private suspend fun f(): I = I(suspendHere<String>())

    fun g() = suspend { f() }
}

fun box(): String {
    var result = "fail"
    suspend { result = C().g()().x as String }.startCoroutine(EmptyContinuation)

    c?.resume("OK")
    return result
}
