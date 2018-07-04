// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var cResult = 0
    suspend fun suspendHere(v: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(v * 2)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Int): Controller {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.cResult = it
    })

    return controller
}

inline fun foo(x: (Int) -> Unit) {
    for (i in 1..2) {
        run {
            x(i)
        }
    }
}

fun box(): String {
    var result = ""

    val controllerResult = builder {
        result += "-"
        foo {
            run {
                result += suspendHere(it).toString()
                if (it == 2) return@builder 56
            }
        }
        // Should be unreachable
        result += "+"
        1
    }.cResult

    if (result != "-24") return "fail 1: $result"
    if (controllerResult != 56) return "fail 2: $controllerResult"

    return "OK"
}
