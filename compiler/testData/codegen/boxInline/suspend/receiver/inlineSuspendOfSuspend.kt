// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// FILE: test.kt

import kotlin.coroutines.*
import helpers.*

// Block is allowed to be called inside the body of owner inline function
// suspend calls possible inside lambda matching to the parameter

class Controller {
    var res = "FAIL 1"

    suspend inline fun test(c: suspend Controller.() -> Unit) {
        c()
    }
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
    val controller = Controller()
    builder(controller) {
        test {
            res = calculate()
        }
    }
    if (controller.res != "OK") return controller.res
    controller.res = "FAIL 2"
    builder(controller) {
        test {
            test {
                test {
                    test {
                        test {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return controller.res
}
