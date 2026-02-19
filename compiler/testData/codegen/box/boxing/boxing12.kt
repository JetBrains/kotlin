// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun foo(x: Number) {
    sb.appendLine(x.toByte())
}

fun box(): String {
    foo(18)
    val nonConst = 18
    foo(nonConst)

    assertEquals("18\n18\n", sb.toString())
    return "OK"
}
