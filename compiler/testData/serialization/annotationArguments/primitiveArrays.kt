package test

annotation class PrimitiveArrays(
        val byteArray: ByteArray
        val charArray: CharArray,
        val shortArray: ShortArray,
        val intArray: IntArray,
        val longArray: LongArray,
        val floatArray: FloatArray,
        val doubleArray: DoubleArray,
        val booleanArray: BooleanArray
)

PrimitiveArrays(
        byteArray = byteArray(-7, 7),
        charArray = charArray('%', 'z'),
        shortArray = shortArray(239),
        intArray = intArray(239017, -1),
        longArray = longArray(123456789123456789L),
        floatArray = floatArray(2.72f, 0f),
        doubleArray = doubleArray(-3.14),
        booleanArray = booleanArray(true, false, true)
)
class C1

PrimitiveArrays(
        byteArray = byteArray(),
        charArray = charArray(),
        shortArray = shortArray(),
        intArray = intArray(),
        longArray = longArray(),
        floatArray = floatArray(),
        doubleArray = doubleArray(),
        booleanArray = booleanArray()
)
class C2
