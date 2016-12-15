// FILE: A.kt
package a

import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere() = CoroutineIntrinsics.suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
        CoroutineIntrinsics.SUSPENDED
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
