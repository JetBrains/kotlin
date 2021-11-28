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

suspend inline fun test(controller: Controller, c: () -> Unit =  { controller.res = "OK" }) {
    c()
}

// FILE: box.kt
import kotlin.coroutines.*
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
