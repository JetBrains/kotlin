// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun foo(): String {
    var a = "OK"
    var i = 0
    val x = suspend {
        suspendHere(a[i++].toString())
    }

    return x() + x.invoke()
}


fun box(): String {
    var result = ""

    suspend {
        result = foo()
    }.startCoroutine(EmptyContinuation)

    return result
}
