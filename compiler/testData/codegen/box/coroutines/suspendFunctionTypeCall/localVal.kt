// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(): String {
    var a = "OK"
    var i = 0
    val x: suspend () -> String = {
        suspendHere(a[i++].toString())
    }

    return x() + x.invoke()
}


fun box(): String {
    var result = ""

    builder {
        result = foo()
    }

    return result
}
