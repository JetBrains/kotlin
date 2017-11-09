fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) {
        throw java.lang.AssertionError("Assertion failed: $actual != $expected")
    }
}

enum class Test {
    OK
}

fun box(): String {
    assertEquals(Test.OK.ordinal, 0)
    assertEquals(Test.OK.name, "OK")
    
    return "OK"
}