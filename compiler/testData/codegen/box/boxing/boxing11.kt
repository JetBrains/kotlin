// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

class Foo(val value: Int?) {
    fun foo() {
        printInt(if (value != null) value else 42)
    }
}

fun box(): String {
    Foo(17).foo()
    Foo(null).foo()

    assertEquals("17\n42\n", sb.toString())
    return "OK"
}
