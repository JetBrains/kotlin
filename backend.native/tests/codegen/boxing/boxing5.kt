package codegen.boxing.boxing5

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    printInt(arg ?: 16)
}

@Test fun runTest() {
    foo(null)
    foo(42)
}