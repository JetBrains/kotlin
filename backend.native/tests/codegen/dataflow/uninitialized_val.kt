package codegen.dataflow.uninitialized_val

import kotlin.test.*

fun foo(b: Boolean): Int {
    val x: Int
    if (b) {
        x = 1
    } else {
        x = 2
    }

    return x
}

@Test fun runTest() {
    val uninitializedUnused: Int

    println(foo(true))
    println(foo(false))
}