package codegen.function.defaults9

import kotlin.test.*

class Foo {
    inner class Bar(x: Int, val y: Int = 1) {
        constructor() : this(42)
    }
}

@Test fun runTest() = println(Foo().Bar().y)
