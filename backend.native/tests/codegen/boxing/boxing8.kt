package codegen.boxing.boxing8

import kotlin.test.*

fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

@Test fun runTest() {
    foo(1, null, true, "Hello")
}