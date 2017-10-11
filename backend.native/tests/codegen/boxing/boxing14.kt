package codegen.boxing.boxing14

import kotlin.test.*

@Test fun runTest() {
    42.println()
}

fun <T> T.println() = println(this.toString())