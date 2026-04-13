// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: ANDROID
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
