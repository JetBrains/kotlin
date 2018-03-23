package codegen.inline.getClass

import kotlin.test.*

fun foo() {
    val cls1: Any? = Int
    val cls2: Any? = null

    cls1?.let {
        cls2?.let {
            var itClass = it::class
        }
    }
}

@Test fun runTest() {
    println("OK")
}
