package codegen.boxing.when6

import kotlin.test.*

fun foo() {
}

@Test fun runTest() {
    if (true) foo() else foo()
}