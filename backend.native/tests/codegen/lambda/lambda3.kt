package codegen.lambda.lambda3

import kotlin.test.*

@Test fun runTest() {
    var str = "lambda"
    run {
        println(str)
    }
}

fun run(f: () -> Unit) {
    f()
}