package codegen.`object`.initialization1

import kotlin.test.*

class TestClass {
    constructor() {
        println("constructor1")
    }

    constructor(x: Int) : this() {
        println("constructor2")
    }

    init {
        println("init")
    }

    val f = println("field")
}

@Test fun runTest() {
    TestClass()
    TestClass(1)
}