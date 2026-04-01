// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    sb.appendLine(y)
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    val z = Z()
    z.foo()
    assertEquals("42\n", sb.toString())
    return "OK"
}
