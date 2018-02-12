package codegen.enum.nested

import kotlin.test.*

enum class Foo {
    A;
    enum class Bar { C }
}

@Test fun runTest() {
    println(Foo.A)
    println(Foo.Bar.C)
}