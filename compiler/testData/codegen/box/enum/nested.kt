// WITH_STDLIB

import kotlin.test.*

enum class Foo {
    A;
    enum class Bar { C }
}

fun box(): String {
    assertEquals("A", Foo.A.toString())
    assertEquals("C", Foo.Bar.C.toString())

    return "OK"
}
