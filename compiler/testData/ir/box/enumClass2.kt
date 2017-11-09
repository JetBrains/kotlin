fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) {
        throw java.lang.AssertionError("Assertion failed: $actual != $expected")
    }
}

enum class TestEnumClass(val x: Int) {
    ZERO
//    {
//        init {
//        }
//    }
    ;
    constructor(): this(0)
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.x, 0)

    return "OK"
}