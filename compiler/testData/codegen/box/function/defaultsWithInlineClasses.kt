// WITH_STDLIB

import kotlin.test.*

inline class Foo(val value: Int)
fun foo(x: Foo = Foo(42)) = x.value

fun box(): String {
    assertEquals(foo(), 42)
    assertEquals(foo(Foo(17)), 17)

    return "OK"
}
