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
    val intArr = intArrayOf(5, 2, 1, 9, 80, Int.MIN_VALUE, Int.MAX_VALUE)
    intArr.sort()
    assertArrayNotSameButEquals(intArrayOf(Int.MIN_VALUE, 1, 2, 5, 9, 80, Int.MAX_VALUE), intArr)
    intArr.sortDescending()
    assertArrayNotSameButEquals(intArrayOf(Int.MAX_VALUE, 80, 9, 5, 2, 1, Int.MIN_VALUE), intArr)

    val longArr = longArrayOf(200, 2, 1, 4, 3, Long.MIN_VALUE, Long.MAX_VALUE)
    longArr.sort()
    assertArrayNotSameButEquals(longArrayOf(Long.MIN_VALUE, 1, 2, 3, 4, 200, Long.MAX_VALUE), longArr)
    longArr.sortDescending()
    assertArrayNotSameButEquals(longArrayOf(Long.MAX_VALUE, 200, 4, 3, 2, 1, Long.MIN_VALUE), longArr)

    val charArr = charArrayOf('d', 'c', 'E', 'a', '\u0000', '\uFFFF')
    charArr.sort()
    assertArrayNotSameButEquals(charArrayOf('\u0000', 'E', 'a', 'c', 'd', '\uFFFF'), charArr)
    charArr.sortDescending()
    assertArrayNotSameButEquals(charArrayOf('\uFFFF', 'd', 'c', 'a', 'E', '\u0000'), charArr)


    val strArr = arrayOf("9", "80", "all", "Foo")
    strArr.sort()
    assertArrayNotSameButEquals(arrayOf("80", "9", "Foo", "all"), strArr)
    strArr.sortDescending()
    assertArrayNotSameButEquals(arrayOf("all", "Foo", "9", "80"), strArr)
}
