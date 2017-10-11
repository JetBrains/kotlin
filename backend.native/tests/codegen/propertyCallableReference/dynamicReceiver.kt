package codegen.propertyCallableReference.dynamicReceiver

import kotlin.test.*

class TestClass {
    var x: Int = 42
}

fun foo(): TestClass {
    println(42)
    return TestClass()
}

@Test fun runTest() {
    foo()::x
}