// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Int?) {
    if (arg != null)
        printInt(arg)
}

fun box(): String {
    foo(42)
    val nonConst = 42
    foo(nonConst)

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}
