fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) {
        throw java.lang.AssertionError("Assertion failed: $actual != $expected")
    }
}

enum class TestEnumClass {
    ZERO {
        override fun describe() = "nothing"
    };
    abstract fun describe(): String
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.describe(), "nothing")
    return "OK"
}