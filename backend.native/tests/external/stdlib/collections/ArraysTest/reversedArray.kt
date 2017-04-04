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
    assertArrayNotSameButEquals(intArrayOf(3, 2, 1), intArrayOf(1, 2, 3).reversedArray())
    assertArrayNotSameButEquals(byteArrayOf(3, 2, 1), byteArrayOf(1, 2, 3).reversedArray())
    assertArrayNotSameButEquals(shortArrayOf(3, 2, 1), shortArrayOf(1, 2, 3).reversedArray())
    assertArrayNotSameButEquals(longArrayOf(3, 2, 1), longArrayOf(1, 2, 3).reversedArray())
    assertArrayNotSameButEquals(floatArrayOf(3F, 2F, 1F), floatArrayOf(1F, 2F, 3F).reversedArray())
    assertArrayNotSameButEquals(doubleArrayOf(3.0, 2.0, 1.0), doubleArrayOf(1.0, 2.0, 3.0).reversedArray())
    assertArrayNotSameButEquals(charArrayOf('3', '2', '1'), charArrayOf('1', '2', '3').reversedArray())
    assertArrayNotSameButEquals(booleanArrayOf(false, false, true), booleanArrayOf(true, false, false).reversedArray())
    assertArrayNotSameButEquals(arrayOf("3", "2", "1"), arrayOf("1", "2", "3").reversedArray())
    assertArrayNotSameButEquals(arrayOf("3", "2", "1"), (arrayOf("1", "2", "3") as Array<out String>).reversedArray())
}
