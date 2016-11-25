// IGNORE_BACKEND: JS
// MODULE: controller
// FILE: controller.kt
package lib

class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

// MODULE: main(controller)
// FILE: main.kt
import lib.*

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
