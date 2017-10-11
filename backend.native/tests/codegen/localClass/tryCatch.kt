package codegen.localClass.tryCatch

import kotlin.test.*

private fun foo() {
    val local =
            object {
                fun bar() {
                    try {
                    } catch (t: Throwable) {
                        println(t)
                    }
                }
            }
    local.bar()
}

@Test fun runTest() {
    foo()
}