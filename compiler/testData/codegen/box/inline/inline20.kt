// WITH_STDLIB

// FILE: lib.kt
@Suppress("NOTHING_TO_INLINE")
inline fun bar(block: () -> String) : String {
    return block()
}

// FILE: main.kt
import kotlin.test.*

fun bar2() : String {
    return bar { return "OK" }
}

fun box(): String {
    return bar2()
}
