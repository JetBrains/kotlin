fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) {
        throw java.lang.AssertionError("Assertion failed: $actual != $expected")
    }
}

val x = 1
val y = x + 1

fun box(): String {
    assertEquals(x, 1)
    assertEquals(y, 2)

    return "OK"
}