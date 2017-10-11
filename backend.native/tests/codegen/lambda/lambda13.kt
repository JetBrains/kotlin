package codegen.lambda.lambda13

import kotlin.test.*

@Test fun runTest() {
    apply("foo") {
        println(this)
    }
}

fun apply(str: String, block: String.() -> Unit) {
    str.block()
}