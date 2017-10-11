package codegen.initializers.correctOrder1

import kotlin.test.*

class TestClass {
    val x: Int

    init {
        x = 42
    }

    val y = x
}

@Test fun runTest() {
    println(TestClass().y)
}