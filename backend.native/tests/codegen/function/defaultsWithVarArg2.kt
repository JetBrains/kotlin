package codegen.function.defaultsWithVarArg2

import kotlin.test.*

fun foo(vararg arr: Int = intArrayOf(1, 2)) {
    arr.forEach { println(it) }
}

@Test fun runTest() {
    foo()
    foo(42)
}