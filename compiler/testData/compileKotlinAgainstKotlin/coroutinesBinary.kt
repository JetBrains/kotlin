// COMMON_COROUTINES_TEST
// FILE: A.kt
// WITH_RUNTIME
// WITH_COROUTINES
package a

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    var callback: () -> Unit = {}
    suspend fun suspendHere() = suspendCoroutine<String> { x ->
        callback = {
            x.resume("OK")
        }
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    val controller = Controller()
    c.startCoroutine(controller, object : helpers.ContinuationAdapter<Unit>() {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resume(value: Unit) {}

        override fun resumeWithException(exception: Throwable) {}
    })

    controller.callback()
}

// FILE: B.kt
import a.builder

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
