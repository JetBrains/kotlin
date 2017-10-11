package codegen.boxing.boxing6

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Any) {
    printInt(arg as? Int ?: 16)
}

@Test fun runTest() {
    foo(42)
    foo("Hello")
}