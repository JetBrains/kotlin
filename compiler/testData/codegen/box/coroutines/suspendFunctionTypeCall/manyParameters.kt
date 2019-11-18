// IGNORE_BACKEND_FIR: JVM_IR
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

suspend fun foo(c: suspend Double.(Long, Int, String) -> String) = (1.0).c(56L, 55, "abc")

fun box(): String {
    var result = ""
    var final = ""

    builder {
        final = foo { l, i, s ->
            result = suspendHere("$this#$l#$i#$s")
            "OK"
        }
    }

    if (result != "1.0#56#55#abc" && result != "1#56#55#abc") return "fail: $result"

    return final
}
