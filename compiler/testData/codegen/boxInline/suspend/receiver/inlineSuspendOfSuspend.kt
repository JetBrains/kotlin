// FILE: test.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

import kotlin.coroutines.experimental.*

// Block is allowed to be called inside the body of owner inline function
// suspend calls possible inside lambda matching to the parameter

class Controller {
    var res = "FAIL 1"

    suspend inline fun test(c: suspend Controller.() -> Unit) {
        c()
    }
}

// FILE: box.kt

import kotlin.coroutines.experimental.*

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
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
