// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported
// WITH_STDLIB

// FILE: lib.kt
inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}


// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    while (true) {
        "".let { it.run { break } }
    }

    return "OK"
}