package codegen.initializers.correctOrder2

import kotlin.test.*

class TestClass {
    val x: Int

    val y = 42

    init {
        x = y
    }
}

@Test fun runTest() {
    println(TestClass().x)
}