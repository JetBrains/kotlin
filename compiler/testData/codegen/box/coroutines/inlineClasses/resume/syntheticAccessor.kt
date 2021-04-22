// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@Suppress("UNSUPPORTED_FEATURE")
inline class I(val x: Any?)

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { c ->
    c.resume(Unit)
    COROUTINE_SUSPENDED
}

class C {
    private suspend fun f(): I {
        suspendHere()
        return I("OK")
    }

    fun g() = suspend { f() }
}

fun box(): String {
    var result = "fail"
    suspend { result = C().g()().x as String }.startCoroutine(EmptyContinuation)
    return result
}
