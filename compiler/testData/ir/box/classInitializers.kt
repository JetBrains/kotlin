fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) {
        throw java.lang.AssertionError("Assertion failed: $actual != $expected")
    }
}

class Test(val x: Int) {
    val y = x + 1
    val z: Int
    init {
        z = y + 1
    }
}

fun box(): String {
    val test = Test(1)
    assertEquals(test.x, 1)
    assertEquals(test.y, 2)
    assertEquals(test.z, 3)

    return "OK"
}