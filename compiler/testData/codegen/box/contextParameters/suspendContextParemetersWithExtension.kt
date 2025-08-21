// KT-76232
// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_COROUTINES
// WITH_STDLIB

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.text.buildString

context(u: Unit, s: String)
public suspend fun Int.foo(): String {
    return buildString {
        append("${this@foo},")
        append("$u,")
        append("$s")
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"

    builder {
        with(Unit) {
            with("foo") {
                result = 42.foo()
            }
        }
    }

    return if (result == "42,kotlin.Unit,foo") "OK" else result
}
