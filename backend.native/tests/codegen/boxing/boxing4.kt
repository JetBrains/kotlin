package codegen.boxing.boxing4

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Any?) {
    if (arg is Int? && arg != null)
        printInt(arg)
}

@Test fun runTest() {
    foo(16)
}