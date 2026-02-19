// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Any) {
    val argAsInt = try {
        arg as Int
    } catch (e: ClassCastException) {
        0
    }
    printInt(argAsInt)
}

fun box(): String {
    foo(1)
    foo("Hello")
    val nonConstInt = 1
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstString)

    assertEquals("1\n0\n1\n0\n", sb.toString())
    return "OK"
}
