// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalCollectionLiterals::class, ExperimentalUnsignedTypes::class)

inline fun <reified T, R> runOnTwoArgs(a: T, b: T, ref: (Array<T>) -> R): R = ref(arrayOf(a, b))

fun runOnTwoIntArgs(a: Int, b: Int, ref: (IntArray) -> IntArray): IntArray = ref(intArrayOf(a, b))
fun runOnTwoByteArgs(a: Byte, b: Byte, ref: (ByteArray) -> ByteArray): ByteArray = ref(byteArrayOf(a, b))
fun runOnTwoShortArgs(a: Short, b: Short, ref: (ShortArray) -> ShortArray): ShortArray = ref(shortArrayOf(a, b))
fun runOnTwoLongArgs(a: Long, b: Long, ref: (LongArray) -> LongArray): LongArray = ref(longArrayOf(a, b))
fun runOnTwoCharArgs(a: Char, b: Char, ref: (CharArray) -> CharArray): CharArray = ref(charArrayOf(a, b))
fun runOnTwoBooleanArgs(a: Boolean, b: Boolean, ref: (BooleanArray) -> BooleanArray): BooleanArray = ref(booleanArrayOf(a, b))
fun runOnTwoFloatArgs(a: Float, b: Float, ref: (FloatArray) -> FloatArray): FloatArray = ref(floatArrayOf(a, b))
fun runOnTwoDoubleArgs(a: Double, b: Double, ref: (DoubleArray) -> DoubleArray): DoubleArray = ref(doubleArrayOf(a, b))

fun runOnTwoUByteArgs(a: UByte, b: UByte, ref: (UByteArray) -> UByteArray): UByteArray = ref(ubyteArrayOf(a, b))
fun runOnTwoUShortArgs(a: UShort, b: UShort, ref: (UShortArray) -> UShortArray): UShortArray = ref(ushortArrayOf(a, b))
fun runOnTwoUIntArgs(a: UInt, b: UInt, ref: (UIntArray) -> UIntArray): UIntArray = ref(uintArrayOf(a, b))
fun runOnTwoULongArgs(a: ULong, b: ULong, ref: (ULongArray) -> ULongArray): ULongArray = ref(ulongArrayOf(a, b))

fun box(): String {
    return when {
        runOnTwoArgs(1, 2, List.Companion::of) != listOf(1, 2) -> "Fail#List"
        runOnTwoArgs(1, 2, MutableList.Companion::of) != mutableListOf(1, 2) -> "Fail#MutableList"
        runOnTwoArgs(42, 42, Set.Companion::of) != setOf(42) -> "Fail#Set"
        runOnTwoArgs(42, 42, MutableSet.Companion::of) != mutableSetOf(42) -> "Fail#MutableSet"
        !runOnTwoArgs(1, 2, Array.Companion::of).contentEquals(arrayOf(1, 2)) -> "Fail#Array"

        !runOnTwoIntArgs(1, 2, IntArray::of).contentEquals(intArrayOf(1, 2)) -> "Fail#IntArray"
        !runOnTwoByteArgs(1.toByte(), 2.toByte(), ByteArray::of).contentEquals(byteArrayOf(1, 2)) -> "Fail#ByteArray"
        !runOnTwoShortArgs(1.toShort(), 2.toShort(), ShortArray::of).contentEquals(shortArrayOf(1, 2)) -> "Fail#ShortArray"
        !runOnTwoLongArgs(1L, 2L, LongArray::of).contentEquals(longArrayOf(1L, 2L)) -> "Fail#LongArray"
        !runOnTwoCharArgs('a', 'b', CharArray::of).contentEquals(charArrayOf('a', 'b')) -> "Fail#CharArray"
        !runOnTwoBooleanArgs(true, false, BooleanArray::of).contentEquals(booleanArrayOf(true, false)) -> "Fail#BooleanArray"
        !runOnTwoFloatArgs(1.0f, 2.0f, FloatArray::of).contentEquals(floatArrayOf(1.0f, 2.0f)) -> "Fail#FloatArray"
        !runOnTwoDoubleArgs(1.0, 2.0, DoubleArray::of).contentEquals(doubleArrayOf(1.0, 2.0)) -> "Fail#DoubleArray"

        !runOnTwoUByteArgs(1u.toUByte(), 2u.toUByte(), UByteArray::of).contentEquals(ubyteArrayOf(1u, 2u)) -> "Fail#UByteArray"
        !runOnTwoUShortArgs(1u.toUShort(), 2u.toUShort(), UShortArray::of).contentEquals(ushortArrayOf(1u, 2u)) -> "Fail#UShortArray"
        !runOnTwoUIntArgs(1u, 2u, UIntArray::of).contentEquals(uintArrayOf(1u, 2u)) -> "Fail#UIntArray"
        !runOnTwoULongArgs(1UL, 2UL, ULongArray::of).contentEquals(ulongArrayOf(1UL, 2UL)) -> "Fail#ULongArray"

        else -> "OK"
    }
}
