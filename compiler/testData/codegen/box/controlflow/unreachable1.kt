// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.append(foo())

    assertEquals("1", sb.toString())
    return "OK"
}

fun foo(): Int {
    return 1
    sb.appendLine("After return")
}
