// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: controller
// FILE: controller.kt
package lib

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}

// MODULE: main(controller, support)
// FILE: main.kt
import lib.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

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
