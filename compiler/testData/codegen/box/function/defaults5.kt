// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class TestClass(val x: Int) {
    fun foo(y: Int = x) {
        sb.appendLine(y)
    }
}

fun TestClass.bar(y: Int = x) {
    sb.appendLine(y)
}

fun box(): String {
    TestClass(5).foo()
    TestClass(6).bar()

    assertEquals("5\n6\n", sb.toString())
    return "OK"
}
