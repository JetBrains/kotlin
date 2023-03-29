// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

data class Test1(
        val stringArray: Array<String>,
        val charArray: CharArray,
        val booleanArray: BooleanArray,
        val byteArray: ByteArray,
        val shortArray: ShortArray,
        val intArray: IntArray,
        val longArray: LongArray,
        val floatArray: FloatArray,
        val doubleArray: DoubleArray
)

data class Test2<T>(
        val genericArray: Array<T>
)

data class Test3(
        val anyArrayN: Array<Any>?
)
