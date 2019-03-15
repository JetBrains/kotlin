// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun <T> suspendHere(v: T): T = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

var result: Any = ""

fun <T : Any> foo(v: T) {
    builder {
        val r = suspendHere(v)
        suspendHere("")
        result = r
    }
}

fun box(): String {
    foo("OK")
    return result as String
}
