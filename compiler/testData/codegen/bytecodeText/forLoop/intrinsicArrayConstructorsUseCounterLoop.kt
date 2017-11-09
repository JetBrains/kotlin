fun Int.toTrue() = true

fun testBooleanArray(n: Int) =
        BooleanArray(n) { it.toTrue() }

fun testByteArray(n: Int) =
        ByteArray(n) { it.toByte() }

fun testShortArray(n: Int) =
        ShortArray(n) { it.toShort() }

fun testIntArray(n: Int) =
        IntArray(n) { it }

fun testLongArray(n: Int) =
        LongArray(n) { it.toLong() }

fun testFloatArray(n: Int) =
        FloatArray(n) { it.toFloat() }

fun testDoubleArray(n: Int) =
        DoubleArray(n) { it.toDouble() }

fun testObjectArray(n: Int) =
        Array(n) { it as Any }

// 0 IF_ICMPGT
// 0 IF_CMPEQ
// 8 IF_ICMPGE