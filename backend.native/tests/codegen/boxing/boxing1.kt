package codegen.boxing.boxing1

import kotlin.test.*

fun foo(arg: Any) {
    println(arg.toString())
}

@Test fun runTest() {
    foo(1)
    foo(false)
    foo("Hello")
}