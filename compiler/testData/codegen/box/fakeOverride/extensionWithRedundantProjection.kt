// WITH_STDLIB

class ValueWrapper<out T>(val value: T)

interface TestInterface {
    fun <T> ValueWrapper<out T>.checkValue(v: T) = v == value
}

class TestClass : TestInterface {
    fun doTest() = ValueWrapper("Hello").checkValue("Hello")
}

fun box(): String {
    if (TestClass().doTest()) {
        return "OK"
    }
    return "Fail"
}
