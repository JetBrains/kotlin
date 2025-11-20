// WITH_STDLIB

import kotlin.test.*

class TestClass {
    val x: Int

    init {
        x = 42
    }

    val y = x
}

fun box(): String {
    assertEquals(42, TestClass().y)
    return "OK"
}
