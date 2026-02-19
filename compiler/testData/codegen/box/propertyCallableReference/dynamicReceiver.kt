// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class TestClass {
    var x: Int = 42
}

fun foo(): TestClass {
    sb.append(42)
    return TestClass()
}

fun box(): String {
    foo()::x

    assertEquals("42", sb.toString())
    return "OK"
}
