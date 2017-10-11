package codegen.lambda.lambda12

import kotlin.test.*

@Test fun runTest() {
    val lambda = { s1: String, s2: String ->
        println(s1)
        println(s2)
    }

    lambda("one", "two")
}