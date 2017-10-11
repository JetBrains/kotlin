package codegen.lambda.lambda10

import kotlin.test.*

@Test fun runTest() {
    var str = "original"

    val lambda = {
        println(str)
    }

    lambda()

    str = "changed"
    lambda()
}