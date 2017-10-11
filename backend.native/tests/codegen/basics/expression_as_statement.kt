package codegen.basics.expression_as_statement

import kotlin.test.*

fun foo() {
    Any() as String
}

@Test
fun runTest() {
    try {
        foo()
    } catch (e: Throwable) {
        println("Ok")
        return
    }

    println("Fail")
}