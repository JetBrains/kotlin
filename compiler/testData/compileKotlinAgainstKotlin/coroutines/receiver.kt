// FILE: A.kt
// LANGUAGE_VERSION: 1.2
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JS, NATIVE, JVM_IR, JS_IR
import kotlin.coroutines.experimental.*

var callback: (() -> Unit)? = null
fun join() {
    while (callback != null) {
        val x = callback!!
        callback = null
        x()
    }
}

fun (suspend () -> String).builder(): String {
    var res = "FAIL"
    startCoroutine(object : Continuation<String> {
        override val context = EmptyCoroutineContext
        override fun resume(value: String) {
            res = value
        }
        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })

    join()

    return res
}

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun ok(): String {
    return o() + k()
}

suspend fun o(): String = suspendCoroutine { continuation ->
    callback = { continuation.resume("O") }
}

suspend fun k(): String = suspendCoroutine { continuation ->
    callback = { continuation.resume("K") }
}

fun box(): String {
    if ((::ok).builder() != "OK") return "FAIL 1"
    if (suspend { ok() }.builder() != "OK") return "FAIL 2"
    return "OK"
}
