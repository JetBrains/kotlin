// WITH_STDLIB

import kotlin.test.*

class Foo {
    fun test(x: Int = 1) = x
}

class Bar {
    fun test(x: Int = 2) = x
}

fun box(): String {
    assertEquals(2, Bar().test())

    return "OK"
}
