package runtime.basic.initializers5

import kotlin.test.*

object A {
    val a = 42
    val b = A.a
}

@Test fun runTest() {
    println(A.b)
}