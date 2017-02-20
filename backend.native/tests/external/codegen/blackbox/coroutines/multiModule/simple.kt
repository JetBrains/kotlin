// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: controller
// FILE: controller.kt
package lib

import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume("OK")
        CoroutineIntrinsics.SUSPENDED
    }
}

// MODULE: main(controller, support)
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
