package codegen.function.defaults6

import kotlin.test.*

open class Foo(val x: Int = 42)
class Bar : Foo()

@Test fun runTest() {
    println(Bar().x)
}