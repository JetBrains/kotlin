// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Any?) {
    if (arg is Int? && arg != null)
        printInt(arg)
}

fun box(): String {
    foo(16)
    val nonConst = 16
    foo(nonConst)

    assertEquals("16\n16\n", sb.toString())
    return "OK"
}
