package codegen.lambda.lambda1

import kotlin.test.*

@Test fun runTest() {
    run {
        println("lambda")
    }
}

fun run(f: () -> Unit) {
    f()
}