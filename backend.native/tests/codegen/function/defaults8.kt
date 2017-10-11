package codegen.function.defaults8

import kotlin.test.*

class Foo {
    fun test(x: Int = 1) = x
}

class Bar {
    fun test(x: Int = 2) = x
}

@Test fun runTest() {
    println(Bar().test())
}