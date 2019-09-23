class TestClass {
    fun fn_true() = true
}

fun box(): String {
    val testClass = TestClass()
    if (testClass.fn_true()) {
        return "OK"
    } else {
        return "FAIL"
    }
}