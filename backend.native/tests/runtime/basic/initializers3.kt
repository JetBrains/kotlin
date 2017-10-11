package runtime.basic.initializers3

import kotlin.test.*

class Foo(val bar: Int)

var x = Foo(42)

@Test fun runTest() {
    println(x.bar)
}