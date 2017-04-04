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
    booleanArrayOf(true, false, true).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    byteArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    shortArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    intArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    longArrayOf(0, 1, 2, 3, 4, 5).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    floatArrayOf(0F, 1F, 2F, 3F).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0).let { assertArrayNotSameButEquals(it, it.copyOf()) }
    charArrayOf('0', '1', '2', '3', '4', '5').let { assertArrayNotSameButEquals(it, it.copyOf()) }
}
