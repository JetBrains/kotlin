// WITH_STDLIB

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun bar(block: () -> String) : String {
    return block()
}

fun bar2() : String {
    return bar { return "OK" }
}

fun box(): String {
    return bar2()
}
