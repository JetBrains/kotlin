// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: controller
// FILE: controller.kt
package lib

import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        SUSPENDED
    }
}

// MODULE: main(controller)
// FILE: main.kt
import lib.*
import kotlin.coroutines.*

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
