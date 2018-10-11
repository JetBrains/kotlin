// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var res = ""

suspend fun foo(y: A?): String {
    val origin: String? = y?.z
    if (origin != null) {
        baz(origin)
        baz(origin)
    }

    return res
}

suspend fun baz(y: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    res += y[res.length]
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

class A(val z: String)

fun box(): String {
    var result = ""

    builder {
        result = foo(A("OK"))
    }

    return result
}
