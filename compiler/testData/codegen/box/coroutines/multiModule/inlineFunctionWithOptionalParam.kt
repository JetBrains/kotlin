// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: lib.kt
inline fun foo(x: String = "OK"): String {
    return x + x
}

// MODULE: main(lib, support)
// FILE: main.kt
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = ""

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resume(value: Unit) {
        }
        override fun resumeWithException(exception: Throwable) {
        }
    })
}

fun box(): String {
    builder {
        result = foo()
    }
    if (result != "OKOK") return "fail: $result"
    return "OK"
}
