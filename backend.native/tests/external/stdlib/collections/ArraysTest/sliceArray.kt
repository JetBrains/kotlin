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
    val coll: Collection<Int> = listOf(3, 1, 2)

    assertArrayNotSameButEquals(arrayOf("B"), arrayOf("A", "B", "C").sliceArray(1..1))
    assertArrayNotSameButEquals(arrayOf("B"), (arrayOf("A", "B", "C") as Array<out String>).sliceArray(1..1))
    assertArrayNotSameButEquals(arrayOf('E', 'B', 'C'), arrayOf('A', 'B', 'C', 'E').sliceArray(coll))


    assertArrayNotSameButEquals(arrayOf<Int>(), arrayOf<Int>().sliceArray(5..4))
    assertArrayNotSameButEquals(intArrayOf(), intArrayOf(1, 2, 3).sliceArray(5..1))
    assertArrayNotSameButEquals(intArrayOf(2, 3, 9), intArrayOf(2, 3, 9, 2, 3, 9).sliceArray(coll))
    assertArrayNotSameButEquals(doubleArrayOf(2.0, 3.0), doubleArrayOf(2.0, 3.0, 9.0).sliceArray(0..1))
    assertArrayNotSameButEquals(floatArrayOf(2f, 3f), floatArrayOf(2f, 3f, 9f).sliceArray(0..1))
//        assertArrayNotSameButEquals(byteArrayOf(127, 100), byteArrayOf(50, 100, 127).sliceArray(2 downTo 1))
//        assertArrayNotSameButEquals(shortArrayOf(200, 100), shortArrayOf(50, 100, 200).sliceArray(2 downTo 1))
    assertArrayNotSameButEquals(longArrayOf(100L, 200L, 30L), longArrayOf(50L, 100L, 200L, 30L).sliceArray(1..3))
    assertArrayNotSameButEquals(booleanArrayOf(true, false, true), booleanArrayOf(true, false, true, true).sliceArray(coll))
}
