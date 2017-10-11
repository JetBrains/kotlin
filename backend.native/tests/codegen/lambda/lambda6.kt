package codegen.lambda.lambda6

import kotlin.test.*

@Test fun runTest() {
    val str = "captured"
    foo {
        println(it)
        println(str)
    }
}

fun foo(f: (Int) -> Unit) {
    f(42)
}