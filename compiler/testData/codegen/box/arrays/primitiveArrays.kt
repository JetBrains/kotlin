// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.*

fun box(): String {
    assertTrue(eqBoolean(booleanArrayOf(false), BooleanArray(1)))
    assertTrue(eqBoolean(booleanArrayOf(false, true, false), BooleanArray(3) { it % 2 != 0 }))
    assertTrue(eqBoolean(booleanArrayOf(true), booleanArrayOf(true).copyOf()))
    assertTrue(eqBoolean(booleanArrayOf(true, false), booleanArrayOf(true).copyOf(2)))
    assertTrue(eqBoolean(booleanArrayOf(true), booleanArrayOf(true, true).copyOf(1)))
    assertTrue(eqBoolean(booleanArrayOf(false, true), booleanArrayOf(false) + true))
    assertTrue(eqBoolean(booleanArrayOf(false, true, false), booleanArrayOf(false) + listOf(true, false)))
    assertTrue(eqBoolean(booleanArrayOf(true, false), booleanArrayOf(false, true, false, true).copyOfRange(1, 3)))
    assertTrue(eqBoolean(booleanArrayOf(false, true, false, true), customBooleanArrayOf(false, *booleanArrayOf(true, false), true)))
    assertTrue(booleanArrayOf(true).iterator() is BooleanIterator)
    assertEquals(true, booleanArrayOf(true).iterator().nextBoolean())
    assertEquals(true, booleanArrayOf(true).iterator().next())
    assertFalse(booleanArrayOf().iterator().hasNext())
    assertTrue(assertFails { booleanArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqByte(byteArrayOf(0), ByteArray(1)))
    assertTrue(eqByte(byteArrayOf(1, 2, 3), ByteArray(3) { (it + 1).toByte() }))
    assertTrue(eqByte(byteArrayOf(1), byteArrayOf(1).copyOf()))
    assertTrue(eqByte(byteArrayOf(1, 0), byteArrayOf(1).copyOf(2)))
    assertTrue(eqByte(byteArrayOf(1), byteArrayOf(1, 2).copyOf(1)))
    assertTrue(eqByte(byteArrayOf(1, 2), byteArrayOf(1) + 2))
    assertTrue(eqByte(byteArrayOf(1, 2, 3), byteArrayOf(1) + listOf(2.toByte(), 3.toByte())))
    assertTrue(eqByte(byteArrayOf(2, 3), byteArrayOf(1, 2, 3, 4).copyOfRange(1, 3)))
    assertTrue(eqByte(byteArrayOf(1, 2, 3, 4), customByteArrayOf(1.toByte(), *byteArrayOf(2, 3), 4.toByte())))
    assertTrue(byteArrayOf(1).iterator() is ByteIterator)
    assertEquals(1, byteArrayOf(1).iterator().nextByte())
    assertEquals(1, byteArrayOf(1).iterator().next())
    assertFalse(byteArrayOf().iterator().hasNext())
    assertTrue(assertFails { byteArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqShort(shortArrayOf(0), ShortArray(1)))
    assertTrue(eqShort(shortArrayOf(1, 2, 3), ShortArray(3) { (it + 1).toShort() }))
    assertTrue(eqShort(shortArrayOf(1), shortArrayOf(1).copyOf()))
    assertTrue(eqShort(shortArrayOf(1, 0), shortArrayOf(1).copyOf(2)))
    assertTrue(eqShort(shortArrayOf(1), shortArrayOf(1, 2).copyOf(1)))
    assertTrue(eqShort(shortArrayOf(1, 2), shortArrayOf(1) + 2))
    assertTrue(eqShort(shortArrayOf(1, 2, 3), shortArrayOf(1) + listOf(2.toShort(), 3.toShort())))
    assertTrue(eqShort(shortArrayOf(2, 3), shortArrayOf(1, 2, 3, 4).copyOfRange(1, 3)))
    assertTrue(eqShort(shortArrayOf(1, 2, 3, 4), customShortArrayOf(1.toShort(), *shortArrayOf(2, 3), 4.toShort())))
    assertTrue(shortArrayOf(1).iterator() is ShortIterator)
    assertEquals(1, shortArrayOf(1).iterator().nextShort())
    assertEquals(1, shortArrayOf(1).iterator().next())
    assertFalse(shortArrayOf().iterator().hasNext())
    assertTrue(assertFails { shortArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqChar(charArrayOf(0.toChar()), CharArray(1)))
    assertTrue(eqChar(charArrayOf('a', 'b', 'c'), CharArray(3) { 'a' + it }))
    assertTrue(eqChar(charArrayOf('a'), charArrayOf('a').copyOf()))
    assertTrue(eqChar(charArrayOf('a', 0.toChar()), charArrayOf('a').copyOf(2)))
    assertTrue(eqChar(charArrayOf('a'), charArrayOf('a', 'b').copyOf(1)))
    assertTrue(eqChar(charArrayOf('a', 'b'), charArrayOf('a') + 'b'))
    assertTrue(eqChar(charArrayOf('a', 'b', 'c'), charArrayOf('a') + listOf('b', 'c')))
    assertTrue(eqChar(charArrayOf('b', 'c'), charArrayOf('a', 'b', 'c', 'd').copyOfRange(1, 3)))
    assertTrue(eqChar(charArrayOf('a', 'b', 'c', 'd'), customCharArrayOf('a', *charArrayOf('b', 'c'), 'd')))
    assertTrue(charArrayOf('a').iterator() is CharIterator)
    assertEquals('a', charArrayOf('a').iterator().nextChar())
    assertEquals('a', charArrayOf('a').iterator().next())
    assertFalse(charArrayOf().iterator().hasNext())
    assertTrue(assertFails { charArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqInt(intArrayOf(0), IntArray(1)))
    assertTrue(eqInt(intArrayOf(1, 2, 3), IntArray(3) { it + 1 }))
    assertTrue(eqInt(intArrayOf(1), intArrayOf(1).copyOf()))
    assertTrue(eqInt(intArrayOf(1, 0), intArrayOf(1).copyOf(2)))
    assertTrue(eqInt(intArrayOf(1), intArrayOf(1, 2).copyOf(1)))
    assertTrue(eqInt(intArrayOf(1, 2), intArrayOf(1) + 2))
    assertTrue(eqInt(intArrayOf(1, 2, 3), intArrayOf(1) + listOf(2, 3)))
    assertTrue(eqInt(intArrayOf(2, 3), intArrayOf(1, 2, 3, 4).copyOfRange(1, 3)))
    assertTrue(eqInt(intArrayOf(1, 2, 3, 4), customIntArrayOf(1, *intArrayOf(2, 3), 4)))
    assertTrue(intArrayOf(1).iterator() is IntIterator)
    assertEquals(1, intArrayOf(1).iterator().nextInt())
    assertEquals(1, intArrayOf(1).iterator().next())
    assertFalse(intArrayOf().iterator().hasNext())
    assertTrue(assertFails { intArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqFloat(floatArrayOf(0f), FloatArray(1)))
    assertTrue(eqFloat(floatArrayOf(1f, 2f, 3f), FloatArray(3) { (it + 1).toFloat() }))
    assertTrue(eqFloat(floatArrayOf(1f), floatArrayOf(1f).copyOf()))
    assertTrue(eqFloat(floatArrayOf(1f, 0f), floatArrayOf(1f).copyOf(2)))
    assertTrue(eqFloat(floatArrayOf(1f), floatArrayOf(1f, 2f).copyOf(1)))
    assertTrue(eqFloat(floatArrayOf(1f, 2f), floatArrayOf(1f) + 2f))
    assertTrue(eqFloat(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f) + listOf(2f, 3f)))
    assertTrue(eqFloat(floatArrayOf(2f, 3f), floatArrayOf(1f, 2f, 3f, 4f).copyOfRange(1, 3)))
    assertTrue(eqFloat(floatArrayOf(1f, 2f, 3f, 4f), customFloatArrayOf(1f, *floatArrayOf(2f, 3f), 4f)))
    assertTrue(floatArrayOf(1f).iterator() is FloatIterator)
    assertEquals(1f, floatArrayOf(1f).iterator().nextFloat())
    assertEquals(1f, floatArrayOf(1f).iterator().next())
    assertFalse(floatArrayOf().iterator().hasNext())
    assertTrue(assertFails { floatArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqDouble(doubleArrayOf(0.0), DoubleArray(1)))
    assertTrue(eqDouble(doubleArrayOf(1.0, 2.0, 3.0), DoubleArray(3) { (it + 1).toDouble() }))
    assertTrue(eqDouble(doubleArrayOf(1.0), doubleArrayOf(1.0).copyOf()))
    assertTrue(eqDouble(doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0).copyOf(2)))
    assertTrue(eqDouble(doubleArrayOf(1.0), doubleArrayOf(1.0, 2.0).copyOf(1)))
    assertTrue(eqDouble(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0) + 2.0))
    assertTrue(eqDouble(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(1.0) + listOf(2.0, 3.0)))
    assertTrue(eqDouble(doubleArrayOf(2.0, 3.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0).copyOfRange(1, 3)))
    assertTrue(eqDouble(doubleArrayOf(1.0, 2.0, 3.0, 4.0), customDoubleArrayOf(1.0, *doubleArrayOf(2.0, 3.0), 4.0)))
    assertTrue(doubleArrayOf(1.0).iterator() is DoubleIterator)
    assertEquals(1.0, doubleArrayOf(1.0).iterator().nextDouble())
    assertEquals(1.0, doubleArrayOf(1.0).iterator().next())
    assertFalse(doubleArrayOf().iterator().hasNext())
    assertTrue(assertFails { doubleArrayOf().iterator().next() } is NoSuchElementException)

    assertTrue(eqLong(longArrayOf(0), LongArray(1)))
    assertTrue(eqLong(longArrayOf(1, 2, 3), LongArray(3) { it + 1L }))
    assertTrue(eqLong(longArrayOf(1), longArrayOf(1).copyOf()))
    assertTrue(eqLong(longArrayOf(1, 0), longArrayOf(1).copyOf(2)))
    assertTrue(eqLong(longArrayOf(1), longArrayOf(1, 2).copyOf(1)))
    assertTrue(eqLong(longArrayOf(1, 2), longArrayOf(1) + 2))
    assertTrue(eqLong(longArrayOf(1, 2, 3), longArrayOf(1) + listOf(2L, 3L)))
    assertTrue(eqLong(longArrayOf(2, 3), longArrayOf(1, 2, 3, 4).copyOfRange(1, 3)))
    assertTrue(eqLong(longArrayOf(1, 2, 3, 4), customLongArrayOf(1L, *longArrayOf(2, 3), 4L)))
    assertTrue(longArrayOf(1).iterator() is LongIterator)
    assertEquals(1L, longArrayOf(1).iterator().nextLong())
    assertEquals(1L, longArrayOf(1).iterator().next())
    assertFalse(longArrayOf().iterator().hasNext())
    assertTrue(assertFails { longArrayOf().iterator().next() } is NoSuchElementException)

    // If `is` checks work...
    if (intArrayOf() is IntArray) {
        assertTrue(booleanArrayOf(false) is BooleanArray)
        assertTrue(byteArrayOf(0) is ByteArray)
        assertTrue(shortArrayOf(0) is ShortArray)
        assertTrue(charArrayOf('a') is CharArray)
        assertTrue(intArrayOf(0) is IntArray)
        assertTrue(floatArrayOf(0f) is FloatArray)
        assertTrue(doubleArrayOf(0.0) is DoubleArray)
        assertTrue(longArrayOf(0) is LongArray)
    }

    // Rhino `instanceof` fails to distinguish TypedArray's
    if (intArrayOf() is IntArray && (byteArrayOf() as Any) !is IntArray) {
        assertTrue(checkExactArrayType(booleanArrayOf(false), booleanArray = true))
        assertTrue(checkExactArrayType(byteArrayOf(0), byteArray = true))
        assertTrue(checkExactArrayType(shortArrayOf(0), shortArray = true))
        assertTrue(checkExactArrayType(charArrayOf('a'), charArray = true))
        assertTrue(checkExactArrayType(intArrayOf(0), intArray = true))
        assertTrue(checkExactArrayType(floatArrayOf(0f), floatArray = true))
        assertTrue(checkExactArrayType(doubleArrayOf(0.0), doubleArray = true))
        assertTrue(checkExactArrayType(longArrayOf(0), longArray = true))
        assertTrue(checkExactArrayType(arrayOf<Any?>(), array = true))
    }

    return "OK"
}

fun eqBoolean(expected: BooleanArray, actual: BooleanArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqByte(expected: ByteArray, actual: ByteArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqShort(expected: ShortArray, actual: ShortArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqChar(expected: CharArray, actual: CharArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqInt(expected: IntArray, actual: IntArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqLong(expected: LongArray, actual: LongArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqFloat(expected: FloatArray, actual: FloatArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
fun eqDouble(expected: DoubleArray, actual: DoubleArray): Boolean = actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }

fun customBooleanArrayOf(vararg arr: Boolean): BooleanArray = arr
fun customByteArrayOf(vararg arr: Byte): ByteArray = arr
fun customShortArrayOf(vararg arr: Short): ShortArray = arr
fun customCharArrayOf(vararg arr: Char): CharArray = arr
fun customIntArrayOf(vararg arr: Int): IntArray = arr
fun customFloatArrayOf(vararg arr: Float): FloatArray = arr
fun customDoubleArrayOf(vararg arr: Double): DoubleArray = arr
fun customLongArrayOf(vararg arr: Long): LongArray = arr

fun checkExactArrayType(
        a: Any?,
        booleanArray: Boolean = false,
        byteArray: Boolean = false,
        shortArray: Boolean = false,
        charArray: Boolean = false,
        intArray: Boolean = false,
        longArray: Boolean = false,
        floatArray: Boolean = false,
        doubleArray: Boolean = false,
        array: Boolean = false
): Boolean {
    return a is BooleanArray == booleanArray &&
           a is ByteArray == byteArray &&
           a is ShortArray == shortArray &&
           a is CharArray == charArray &&
           a is IntArray == intArray &&
           a is LongArray == longArray &&
           a is FloatArray == floatArray &&
           a is DoubleArray == doubleArray &&
           a is Array<*> == array
}