package test

annotation class PrimitiveArrays(
        val byteArray: ByteArray,
        val charArray: CharArray,
        val shortArray: ShortArray,
        val intArray: IntArray,
        val longArray: LongArray,
        val floatArray: FloatArray,
        val doubleArray: DoubleArray,
        val booleanArray: BooleanArray
)

@PrimitiveArrays(
        byteArray = byteArrayOf(-7, 7),
        charArray = charArrayOf('%', 'z'),
        shortArray = shortArrayOf(239),
        intArray = intArrayOf(239017, -1),
        longArray = longArrayOf(123456789123456789L),
        floatArray = floatArrayOf(2.72f, 0f),
        doubleArray = doubleArrayOf(-3.14),
        booleanArray = booleanArrayOf(true, false, true)
)
class C1

@PrimitiveArrays(
        byteArray = byteArrayOf(),
        charArray = charArrayOf(),
        shortArray = shortArrayOf(),
        intArray = intArrayOf(),
        longArray = longArrayOf(),
        floatArray = floatArrayOf(),
        doubleArray = doubleArrayOf(),
        booleanArray = booleanArrayOf()
)
class C2
