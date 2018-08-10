package codegen.function.defaultsWithInlineClasses

import kotlin.test.*

inline class Foo(val value: Int)
fun foo(x: Foo = Foo(42)) = x.value

@Test fun runTest() {
    assertEquals(foo(), 42)
    assertEquals(foo(Foo(17)), 17)
}