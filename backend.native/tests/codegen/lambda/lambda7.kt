package codegen.lambda.lambda7

import kotlin.test.*

@Test fun runTest() {
    val x = foo {
        it + 1
    }
    println(x)
}

fun foo(f: (Int) -> Int) = f(42)