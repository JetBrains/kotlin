// MODULE: controller
// FILE: controller.kt
package lib

class Controller {
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }
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
