package codegen.boxing.boxing10

import kotlin.test.*

@Test fun runTest() {
    val FALSE: Boolean? = false

    if (FALSE != null) {
        do {
            println("Ok")
        } while (FALSE)
    }
}