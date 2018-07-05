// FILE: A.kt
// LANGUAGE_VERSION: 1.2
import kotlin.coroutines.experimental.*

fun builder(c: suspend () -> String): String {
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
    return res
}

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun ok(continuation: Continuation<String>): Any? {
    return "OK"
}

fun box(): String {
    if (builder(::ok) != "OK") return "FAIL 1"
    if (builder { cont: Continuation<String> -> "OK" } != "OK") return "FAIL 2"
    if (builder(fun (cont: Continuation<String>): Any? = "OK") != "OK") return "FAIL 2"
    return "OK"
}
