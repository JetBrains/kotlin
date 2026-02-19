// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int) {
    sb.appendLine("hello $i4 $i5")
}

// FILE: main.kt
import kotlin.test.*

fun bar(i1: Int, i2: Int) {
    foo(i1, i2)
}

fun box(): String {
    bar(1, 8)

    assertEquals("hello 1 8\n", sb.toString())
    return "OK"
}
