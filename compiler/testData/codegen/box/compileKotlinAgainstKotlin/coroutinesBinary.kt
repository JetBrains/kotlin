// MODULE: lib
// WITH_STDLIB
// WITH_COROUTINES
// FILE: A.kt
package a

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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

// MODULE: main(lib)
// WITH_STDLIB
// FILE: B.kt
import a.builder

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
