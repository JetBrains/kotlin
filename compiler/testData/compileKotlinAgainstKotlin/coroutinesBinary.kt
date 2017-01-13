// FILE: A.kt
package a

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere() = suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), object : Continuation<Unit> {
        override fun resume(value: Unit) {}

        override fun resumeWithException(exception: Throwable) {}
    })
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
