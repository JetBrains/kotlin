// FILE: A.kt
// LANGUAGE_VERSION: 1.2
// TODO: Unmute when automatic conversion experimental <-> release will be implemented
// IGNORE_BACKEND: JVM, JS, NATIVE, JVM_IR, JS_IR

val dummy1: suspend () -> String = { "OK" }
val dummy2: suspend String.() -> String = { this + "K" }
val dummy3: suspend String.(String) -> String = { s -> this + s }

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun box(): String {
//    val continuation = object : Continuation<String> {
//        override val context = EmptyCoroutineContext
//        override fun resume(value: String) {
//        }
//
//        override fun resumeWithException(exception: Throwable) {
//            throw exception
//        }
//    }
//    if (dummy1(continuation) != "OK") return "FAIL 1"
//    if ("O".dummy2(continuation) != "OK") return "FAIL 2"
//    if ("O".dummy3("K", continuation) != "OK") return "FAIL 3"
    return "OK"
}
