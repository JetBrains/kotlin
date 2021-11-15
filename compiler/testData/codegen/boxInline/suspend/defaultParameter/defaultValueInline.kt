// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: test.kt

import kotlin.coroutines.*
import helpers.*

class Controller {
    var res = "FAIL 1"
}

val defaultController = Controller()

suspend inline fun test(controller: Controller = defaultController, c: suspend Controller.() -> Unit) {
    controller.c()
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
    builder {
        test {
            res = calculate()
        }
    }
    if (defaultController.res != "OK") return defaultController.res
    val controller = Controller()
    controller.res = "FAIL 2"
    builder {
        test(controller) {
            res = calculate()
        }
    }
    return controller.res
}
