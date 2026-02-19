// WITH_STDLIB

import kotlin.test.*

class B(val a: Int)

fun B.foo() = this.a

fun box(): String {
    assertEquals(42, B(42).foo())
    return "OK"
}
