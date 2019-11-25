// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun suspendHere(v: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v * 2)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        x(i)
    }
}

fun box(): String {
    var result = ""

    builder {
        result += "-"
        foo {
            result += suspendHere(it).toString()
        }
        result += "+"
    }

    if (result != "-24+") return "fail: $result"

    return "OK"
}
