// WITH_STDLIB
// FILE: lib.kt
inline fun foo2(): Int {
    return try {
        throw Throwable()
    } catch (e: Throwable) {
        return 2
    }
}

// FILE: main.kt
import kotlin.test.*

// The test below is inspired by https://youtrack.jetbrains.com/issue/KT-48876.

fun bar2(): Any {
    return foo2()
}

fun box(): String {
    assertEquals(2, bar2())
    return "OK"
}
