interface TestInterfaceA {
    @JsName("testNameA")
    fun testFunction(): String
}
fun testTestInterfaceA(x: TestInterfaceA) = x.testFunction()

interface TestInterfaceB {
    @JsName("testNameB")
    fun testFunction(): String
}
fun testTestInterfaceB(x: TestInterfaceB) = x.testFunction()

interface TestInterfaceAA: TestInterfaceA {
    override fun testFunction(): String
}
fun testTestInterfaceAA(x: TestInterfaceAA) = x.testFunction()

interface TestInterfaceBAA : TestInterfaceB, TestInterfaceAA {
    override fun testFunction(): String
}
fun testTestInterfaceBAA(x: TestInterfaceBAA) = x.testFunction()

class TestClassBAA : TestInterfaceBAA {
    override fun testFunction(): String = "TestClassBAA"
}
fun testTestClassBAA(x: TestClassBAA) = x.testFunction()

interface TestInterfaceBB: TestInterfaceB {
    override fun testFunction(): String
}
fun testTestInterfaceBB(x: TestInterfaceBB) = x.testFunction()

interface TestInterfaceABB : TestInterfaceA, TestInterfaceBB {
    override fun testFunction(): String
}
fun testTestInterfaceABB(x: TestInterfaceABB) = x.testFunction()

class TestClassABB : TestInterfaceABB {
    override fun testFunction(): String = "TestClassABB"
}
fun testTestClassABB(x: TestClassABB) = x.testFunction()

class TestClassABBAA : TestInterfaceABB, TestInterfaceAA {
    override fun testFunction(): String = "TestClassABBAA"
}
fun testTestClassABBAA(x: TestClassABBAA) = x.testFunction()

fun box(): String {
    TestClassBAA().also {
        assertEquals("TestClassBAA", testTestInterfaceA(it))
        assertEquals("TestClassBAA", testTestInterfaceB(it))

        assertEquals("TestClassBAA", testTestInterfaceAA(it))
        assertEquals("TestClassBAA", testTestInterfaceBAA(it))
        assertEquals("TestClassBAA", testTestClassBAA(it))
    }

    TestClassABB().also {
        assertEquals("TestClassABB", testTestInterfaceA(it))
        assertEquals("TestClassABB", testTestInterfaceB(it))

        assertEquals("TestClassABB", testTestInterfaceBB(it))
        assertEquals("TestClassABB", testTestInterfaceABB(it))
        assertEquals("TestClassABB", testTestClassABB(it))
    }

    TestClassABBAA().also {
        assertEquals("TestClassABBAA", testTestInterfaceA(it))
        assertEquals("TestClassABBAA", testTestInterfaceB(it))

        assertEquals("TestClassABBAA", testTestInterfaceAA(it))
        assertEquals("TestClassABBAA", testTestInterfaceBB(it))
        assertEquals("TestClassABBAA", testTestInterfaceABB(it))

        assertEquals("TestClassABBAA", testTestClassABBAA(it))
    }
    return "OK"
}
