// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

import COROUTINES_PACKAGE.*
import helpers.*

class Controller {
    var res = "FAIL 1"
}

val defaultController = Controller()

suspend inline fun test(controller: Controller, c: () -> Unit =  { controller.res = "OK" }) {
    c()
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}


fun box() : String {
    builder() {
        test(defaultController)
    }
    return defaultController.res
}
