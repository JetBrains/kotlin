// FILE: test.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

import kotlin.coroutines.experimental.*

class Controller {
    var res = "FAIL 1"
}

val defaultController = Controller()

suspend inline fun test(controller: Controller = defaultController, c: suspend Controller.() -> Unit) {
    controller.c()
}

// FILE: box.kt

import kotlin.coroutines.experimental.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
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
