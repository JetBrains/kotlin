enum class TestOk(val x: String = "OK") {
    TEST1,
    TEST2(),
    TEST3("Hello")
}

enum class TestErrors(val x: String) {
    <!NO_VALUE_FOR_PARAMETER!>TEST1,<!>
    TEST2<!NO_VALUE_FOR_PARAMETER!>()<!>,
    TEST3("Hello")
}

enum class TestMultipleConstructors(val x: String = "", val y: Int = 0) {
    TEST;
    constructor(x: String = "") : this(x, 0)
}

enum class TestVarargs(val x: Int) {
    TEST;
    constructor(vararg xs: Any) : this(xs.size)
}
