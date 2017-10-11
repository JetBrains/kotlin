package runtime.basic.initializers1

import kotlin.test.*

class TestClass {
    companion object {
        init {
            println("Init Test")
        }
    }
}

@Test fun runTest() {
    val t1 = TestClass()
    val t2 = TestClass()
    println("Done")
}