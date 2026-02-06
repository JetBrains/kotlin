interface TestInterface {
    @JsName("testName")
    fun testFunction(): String
    fun testFunction(x: String): String
}

interface TestInterfaceA : TestInterface {
    override fun testFunction(): String
    override fun testFunction(x: String): String
}

class TestClassA : TestInterfaceA {
    override fun testFunction(): String = "TestClassA"
    override fun testFunction(x: String): String = "TestClassA: $x"
}

fun testTestInterface1(x: TestInterface) = x.testFunction()
fun testTestInterface2(x: TestInterface) = x.testFunction("OK")

fun testTestInterfaceA1(x: TestInterfaceA) = x.testFunction()
fun testTestInterfaceA2(x: TestInterfaceA) = x.testFunction("OK")

fun testTestClassA1(x: TestClassA) = x.testFunction()
fun testTestClassA2(x: TestClassA) = x.testFunction("OK")

fun box(): String {
    assertEquals("TestClassA", testTestInterface1(TestClassA()))
    assertEquals("TestClassA: OK", testTestInterface2(TestClassA()))

    assertEquals("TestClassA", testTestInterfaceA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestInterfaceA2(TestClassA()))

    assertEquals("TestClassA", testTestClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestClassA2(TestClassA()))

    return "OK"
}
