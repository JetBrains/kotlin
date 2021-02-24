// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// FILE: test.kt

import kotlin.coroutines.*
import helpers.*

class Controller {
    var res = "FAIL 1"
}

val defaultController = Controller()

suspend inline fun test1(controller: Controller = defaultController, crossinline c: suspend Controller.() -> Unit) {
    controller.c()
}

suspend inline fun test2(controller: Controller = defaultController, crossinline c: suspend Controller.() -> Unit) {
    val l : suspend () -> Unit = {
        controller.c()
    }
    l()
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun test3(controller: Controller = defaultController, crossinline c: suspend Controller.() -> Unit) {
    val sr = object: SuspendRunnable {
        override suspend fun run() {
            controller.c()
        }
    }
    sr.run()
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
        test1 {
            res = calculate()
        }
    }
    if (defaultController.res != "OK") return defaultController.res
    defaultController.res = "FAIL 2"
    builder {
        test2 {
            res = calculate()
        }
    }
    if (defaultController.res != "OK") return defaultController.res
    defaultController.res = "FAIL 3"
    builder {
        test3 {
            res = calculate()
        }
    }
    if (defaultController.res != "OK") return defaultController.res
    val controller = Controller()
    controller.res = "FAIL 4"
    builder {
        test1(controller) {
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 5"
    builder {
        test2(controller) {
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 6"
    builder {
        test3(controller) {
            res = calculate()
        }
    }
    return controller.res
}
