package codegen.boxing.boxing9

import kotlin.test.*

fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

fun bar(vararg args: Any?) {
    foo(1, *args, 2, *args, 3)
}

@Test fun runTest() {
    bar(null, true, "Hello")
}