// WITH_STDLIB

import kotlin.test.*

open class Foo(val x: Int = 42)
class Bar : Foo()

fun box(): String {
    assertEquals(42, Bar().x)
    return "OK"
}
