// FILE: A.kt
// LANGUAGE_VERSION: 1.2
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JVM, JS, NATIVE, JVM_IR, JS_IR
import kotlin.coroutines.experimental.*

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
    return res
}

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun ok(continuation: Continuation<String>): Any? {
    return "OK"
}

fun box(): String {
//    if ((::ok).builder() != "OK") return "FAIL 1"
//    if (({ cont: Continuation<String> -> "OK" }).builder() != "OK") return "FAIL 2"
//    if ((fun (cont: Continuation<String>): Any? = "OK").builder() != "OK") return "FAIL 3"
    return "OK"
}
