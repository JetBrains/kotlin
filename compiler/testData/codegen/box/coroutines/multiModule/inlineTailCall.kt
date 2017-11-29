// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: lib
// FILE: lib.kt
suspend inline fun foo(v: String): String = v

suspend inline fun bar(): String = foo("O")

// MODULE: main(lib, support)
// FILE: main.kt
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = bar()
        result += foo("K")
    }

    return result
}
