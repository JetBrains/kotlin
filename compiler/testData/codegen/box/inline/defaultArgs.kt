// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    sb.appendLine(y)
}

fun box(): String {
    val z = Z()
    z.foo()
    assertEquals("42\n", sb.toString())
    return "OK"
}
