package codegen.basics.array_to_any

import kotlin.test.*

@Test
fun runTest() {
    foo().hashCode()
}

fun foo(): Any {
    return Array<Any?>(0, { i -> null })
}