// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: controller
// FILE: controller.kt
package lib

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        SUSPENDED_MARKER
    }
}

// MODULE: main(controller, support)
// FILE: main.kt
import lib.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
