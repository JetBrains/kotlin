// FILE: A.kt
// !LANGUAGE: -ReleaseCoroutines
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JS, NATIVE, JVM_IR, JS_IR

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

var callback: (() -> Unit)? = null
suspend fun dummy(): String = suspendCoroutine {
    callback = { it.resume("OK") }
}

// FILE: B.kt

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(x: suspend () -> Unit) {
    x.startCoroutine(object : Continuation<Any?> {
        override val context: CoroutineContext = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any?>) {
            result.getOrThrow()
        }
    })

    while (callback != null) {
        val x = callback!!
        callback = null
        x()
    }
}

fun box(): String {
    var res = "fail"

    builder {
        res = dummy()
    }

    return res
}
