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
    assertArrayNotSameButEquals(booleanArrayOf(true, false, true), booleanArrayOf(true, false, true, true).copyOfRange(0, 3))
    assertArrayNotSameButEquals(byteArrayOf(0, 1, 2), byteArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
    assertArrayNotSameButEquals(shortArrayOf(0, 1, 2), shortArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
    assertArrayNotSameButEquals(intArrayOf(0, 1, 2), intArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
    assertArrayNotSameButEquals(longArrayOf(0, 1, 2), longArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
    assertArrayNotSameButEquals(floatArrayOf(0F, 1F, 2F), floatArrayOf(0F, 1F, 2F, 3F, 4F, 5F).copyOfRange(0, 3))
    assertArrayNotSameButEquals(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).copyOfRange(0, 3))
    assertArrayNotSameButEquals(charArrayOf('0', '1', '2'), charArrayOf('0', '1', '2', '3', '4', '5').copyOfRange(0, 3))
}
