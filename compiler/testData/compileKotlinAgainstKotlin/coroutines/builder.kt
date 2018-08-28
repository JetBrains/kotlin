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

fun builder1(c: suspend () -> String): String {
    var res = "FAIL"
    c.startCoroutine(object : Continuation<String> {
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

fun builder2(c: suspend String.() -> String): String {
    var res = "FAIL"
    c.startCoroutine("O", object : Continuation<String> {
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
    if (builder1(::ok) != "OK") return "FAIL 1"
    if (builder1 { ok() } != "OK") return "FAIL 2"

    if (builder2 { this + k() } != "OK") return "FAIL 5"
    return "OK"
}
