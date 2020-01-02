// IGNORE_BACKEND_FIR: JVM_IR
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

class Controller {
    var result = ""

    override fun toString() = "Controller"
}

val controller = Controller()

suspend fun foo(c: suspend Controller.(Long, Int, String) -> String) = controller.c(56L, 55, "abc")

fun box(): String {
    var final = ""

    builder {
        final = foo { l, i, s ->
            result = suspendHere("$this#$l#$i#$s")
            "OK"
        }
    }

    if (controller.result != "Controller#56#55#abc") return "fail: ${controller.result}"

    return final
}

