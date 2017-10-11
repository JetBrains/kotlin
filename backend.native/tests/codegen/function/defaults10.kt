package codegen.function.defaults10

import kotlin.test.*

enum class A(one: Int, val two: Int = one) {
    FOO(42)
}

@Test fun runTest() {
    println(A.FOO.two)
}