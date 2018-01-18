package codegen.inline.localFunctionInInitializerBlock

import kotlin.test.*

class Foo {
    init {
        bar()
    }
}

inline fun bar() {
    println({ "Ok" }())
}

@Test fun runTest() {
    Foo()
}