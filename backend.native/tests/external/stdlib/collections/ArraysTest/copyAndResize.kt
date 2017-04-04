import kotlin.test.*

fun <T> assertArrayNotSameButEquals(expected: Array<out T>, actual: Array<out T>, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: IntArray, actual: IntArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: LongArray, actual: LongArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: ShortArray, actual: ShortArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: ByteArray, actual: ByteArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: DoubleArray, actual: DoubleArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: FloatArray, actual: FloatArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: CharArray, actual: CharArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun assertArrayNotSameButEquals(expected: BooleanArray, actual: BooleanArray, message: String = "") {
    assertTrue(expected !== actual && expected contentEquals actual, message)
}

fun box() {
    assertArrayNotSameButEquals(arrayOf("1", "2"), arrayOf("1", "2", "3").copyOf(2))
    assertArrayNotSameButEquals(arrayOf("1", "2", null), arrayOf("1", "2").copyOf(3))

    assertArrayNotSameButEquals(longArrayOf(1, 2), longArrayOf(1, 2, 3).copyOf(2))
    assertArrayNotSameButEquals(longArrayOf(1, 2, 0), longArrayOf(1, 2).copyOf(3))

    assertArrayNotSameButEquals(intArrayOf(1, 2), intArrayOf(1, 2, 3).copyOf(2))
    assertArrayNotSameButEquals(intArrayOf(1, 2, 0), intArrayOf(1, 2).copyOf(3))

    assertArrayNotSameButEquals(charArrayOf('A', 'B'), charArrayOf('A', 'B', 'C').copyOf(2))
    assertArrayNotSameButEquals(charArrayOf('A', 'B', '\u0000'), charArrayOf('A', 'B').copyOf(3))
}
