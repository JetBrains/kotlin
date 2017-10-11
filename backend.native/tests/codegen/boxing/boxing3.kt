package codegen.boxing.boxing3

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    if (arg != null)
        printInt(arg)
}

@Test fun runTest() {
    foo(42)
}