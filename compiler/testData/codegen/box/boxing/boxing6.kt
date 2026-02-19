// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Any) {
    printInt(arg as? Int ?: 16)
}

fun box(): String {
    foo(42)
    foo("Hello")
    val nonConstInt = 42
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstString)

    assertEquals("42\n16\n42\n16\n", sb.toString())
    return "OK"
}
