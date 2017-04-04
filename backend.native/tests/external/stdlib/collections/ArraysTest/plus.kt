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
    assertEquals(listOf("1", "2", "3", "4"), listOf("1", "2") + arrayOf("3", "4"))
    assertArrayNotSameButEquals(arrayOf("1", "2", "3"), arrayOf("1", "2") + "3")
    assertArrayNotSameButEquals(arrayOf("1", "2", "3", "4"), arrayOf("1", "2") + arrayOf("3", "4"))
    assertArrayNotSameButEquals(arrayOf("1", "2", "3", "4"), arrayOf("1", "2") + listOf("3", "4"))
    assertArrayNotSameButEquals(intArrayOf(1, 2, 3), intArrayOf(1, 2) + 3)
    assertArrayNotSameButEquals(intArrayOf(1, 2, 3, 4), intArrayOf(1, 2) + listOf(3, 4))
    assertArrayNotSameButEquals(intArrayOf(1, 2, 3, 4), intArrayOf(1, 2) + intArrayOf(3, 4))
}
