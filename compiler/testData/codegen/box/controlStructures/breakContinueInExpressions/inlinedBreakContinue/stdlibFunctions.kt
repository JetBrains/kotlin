// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}


fun box(): String {
    while (true) {
        "".let { it.run { break } }
    }

    return "OK"
}