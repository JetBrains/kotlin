// FILE: A.kt
// LANGUAGE_VERSION: 1.2
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JVM, JS, NATIVE, JVM_IR, JS_IR
import kotlin.coroutines.experimental.*

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
    return res
}

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun ok(continuation: Continuation<String>): Any? {
    return "OK"
}

fun box(): String {
//    if (builder1(::ok) != "OK") return "FAIL 1"
//    if (builder1 { cont: Continuation<String> -> "OK" } != "OK") return "FAIL 2"
//    if (builder1(fun (cont: Continuation<String>): Any? = "OK") != "OK") return "FAIL 3"
//
//    if (builder2 { cont: Continuation<String> -> this + "K" } != "OK") return "FAIL 5"
//    if (builder2(fun String.(cont: Continuation<String>): Any? = this + "K") != "OK") return "FAIL 6"
    return "OK"
}
