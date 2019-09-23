class TestClassWithProperty {
    val idx_val
        get() = 12

    var idx_var: Int = 0
        get() = 42
}

fun box(): String {
    val testClassWithProperty = TestClassWithProperty()
    if (testClassWithProperty.idx_val == 11) {
        return "FAIL"
    }
    return "OK"
}