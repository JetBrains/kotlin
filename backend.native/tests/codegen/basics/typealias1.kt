package codegen.basics.typealias1

import kotlin.test.*

@Test
fun runTest() {
    println(Bar(42).x)
}

class Foo(val x: Int)
typealias Bar = Foo