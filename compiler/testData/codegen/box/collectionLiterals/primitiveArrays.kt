// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalCollectionLiterals::class)

fun box(): String {
    val int0: IntArray = []
    val int1: IntArray = [42]
    val intM: IntArray = [1, 2, 3]

    val long0: LongArray = []
    val long1: LongArray = [42L]
    val longM: LongArray = [1, 2, 3]

    val short0: ShortArray = []
    val short1: ShortArray = [42]
    val shortM: ShortArray = [1, 2, 3]

    val boolean0: BooleanArray = []
    val boolean1: BooleanArray = [true]
    val booleanM: BooleanArray = [true, false, true]

    val byte0: ByteArray = []
    val byte1: ByteArray = [42]
    val byteM: ByteArray = [1, 2, 3]

    val char0: CharArray = []
    val char1: CharArray = ['*']
    val charM: CharArray = ['a', 'b', 'c']

    val float0: FloatArray = []
    val float1: FloatArray = [42.0f]
    val floatM: FloatArray = [1f, 2f, 3f]

    val double0: DoubleArray = []
    val double1: DoubleArray = [42.0]
    val doubleM: DoubleArray = [1.0, 2.0, 3.0]

    return when {
        !int0.contentEquals(intArrayOf()) -> "Fail#Int0"
        !int1.contentEquals(intArrayOf(42)) -> "Fail#Int1"
        !intM.contentEquals(intArrayOf(1, 2, 3)) -> "Fail#IntM"

        !long0.contentEquals(longArrayOf()) -> "Fail#Long0"
        !long1.contentEquals(longArrayOf(42L)) -> "Fail#Long1"
        !longM.contentEquals(longArrayOf(1, 2, 3)) -> "Fail#LongM"

        !short0.contentEquals(shortArrayOf()) -> "Fail#Short0"
        !short1.contentEquals(shortArrayOf(42)) -> "Fail#Short1"
        !shortM.contentEquals(shortArrayOf(1, 2, 3)) -> "Fail#ShortM"

        !boolean0.contentEquals(booleanArrayOf()) -> "Fail#Boolean0"
        !boolean1.contentEquals(booleanArrayOf(true)) -> "Fail#Boolean1"
        !booleanM.contentEquals(booleanArrayOf(true, false, true)) -> "Fail#BooleanM"

        !byte0.contentEquals(byteArrayOf()) -> "Fail#Byte0"
        !byte1.contentEquals(byteArrayOf(42)) -> "Fail#Byte1"
        !byteM.contentEquals(byteArrayOf(1, 2, 3)) -> "Fail#ByteM"

        !char0.contentEquals(charArrayOf()) -> "Fail#Char0"
        !char1.contentEquals(charArrayOf('*')) -> "Fail#Char1"
        !charM.contentEquals(charArrayOf('a', 'b', 'c')) -> "Fail#CharM"

        !float0.contentEquals(floatArrayOf()) -> "Fail#Float0"
        !float1.contentEquals(floatArrayOf(42.0f)) -> "Fail#Float1"
        !floatM.contentEquals(floatArrayOf(1f, 2f, 3f)) -> "Fail#FloatM"

        !double0.contentEquals(doubleArrayOf()) -> "Fail#Double0"
        !double1.contentEquals(doubleArrayOf(42.0)) -> "Fail#Double1"
        !doubleM.contentEquals(doubleArrayOf(1.0, 2.0, 3.0)) -> "Fail#DoubleM"

        else -> "OK"
    }
}
