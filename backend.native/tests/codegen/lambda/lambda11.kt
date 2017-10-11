package codegen.lambda.lambda11

import kotlin.test.*

@Test fun runTest() {
    val first = "first"
    val second = "second"

    run {
        println(first)
        println(second)
    }
}

fun run(f: () -> Unit) {
    f()
}