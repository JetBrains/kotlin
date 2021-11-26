// WITH_COROUTINES
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt
inline fun foo(x: String = "OK"): String {
    return x + x
}

// MODULE: main(lib, support)
// WITH_STDLIB
// WITH_COROUTINES
// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = ""

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        result = foo()
    }
    if (result != "OKOK") return "fail: $result"
    return "OK"
}
