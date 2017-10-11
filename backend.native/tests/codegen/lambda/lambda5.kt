package codegen.lambda.lambda5

import kotlin.test.*

@Test fun runTest() {
    foo {
        println(it)
    }
}

fun foo(f: (Int) -> Unit) {
    f(42)
}