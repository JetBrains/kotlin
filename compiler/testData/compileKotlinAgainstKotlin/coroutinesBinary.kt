// IGNORE_BACKEND: NATIVE
// FILE: A.kt
// WITH_RUNTIME
package a

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere() = suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
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
