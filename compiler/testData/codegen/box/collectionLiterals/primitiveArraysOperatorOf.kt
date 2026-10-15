// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB

// FILE: imported.kt
import kotlin.IntArray.*

fun testImported(): String {
    val _: IntArray = of()
    if (!of(1, 2).contentEquals(intArrayOf(1, 2))) return "Fail#Imported"
    val ref: (Int) -> IntArray = ::of
    if (!ref(1).contentEquals(intArrayOf(1))) return "Fail#ImportedReference"
    return ""
}

// FILE: full.kt
fun box(): String {
    testImported().let { if (it.isNotEmpty()) return it }

    if (!ByteArray.of().contentEquals(byteArrayOf())) return "Fail#Byte"
    if (!ShortArray.of().contentEquals(shortArrayOf())) return "Fail#Short"
    if (!IntArray.of().contentEquals(intArrayOf())) return "Fail#Int"
    if (!LongArray.of().contentEquals(longArrayOf())) return "Fail#Long"
    if (!FloatArray.of(1f, 2f).contentEquals(floatArrayOf(1f, 2f))) return "Fail#Float"
    if (!DoubleArray.of(1.0, 2.0).contentEquals(doubleArrayOf(1.0, 2.0))) return "Fail#Double"
    if (!CharArray.of('a').contentEquals(charArrayOf('a'))) return "Fail#Char"
    if (!BooleanArray.of(true).contentEquals(booleanArrayOf(true))) return "Fail#Boolean"

    val byte: (ByteArray) -> ByteArray = ByteArray::of
    val short: (ShortArray) -> ShortArray = ShortArray::of
    val int: (IntArray) -> IntArray = IntArray::of
    val long: (LongArray) -> LongArray = LongArray::of
    val float: (FloatArray) -> FloatArray = FloatArray::of
    val double: (DoubleArray) -> DoubleArray = DoubleArray::of
    val char: (CharArray) -> CharArray = CharArray::of
    val boolean: (BooleanArray) -> BooleanArray = BooleanArray::of

    val adaptedByte: (Byte, Byte, Byte) -> ByteArray = ByteArray::of
    val adaptedShort: (Short, Short, Short) -> ShortArray = ShortArray::of
    val adaptedInt: (Int, Int, Int) -> IntArray = IntArray::of
    val adaptedLong: () -> LongArray = LongArray::of
    val adaptedFloat: (Float) -> FloatArray = FloatArray::of
    val adaptedDouble: () -> DoubleArray = DoubleArray::of
    val adaptedChar: (Char) -> CharArray = CharArray::of
    val adaptedBoolean: (Boolean, Boolean, Boolean) -> BooleanArray = BooleanArray::of

    return when {
        !byte(byteArrayOf(1, 2)).contentEquals(byteArrayOf(1, 2)) -> "Fail#ByteReference"
        !short(shortArrayOf(1)).contentEquals(shortArrayOf(1)) -> "Fail#ShortReference"
        !int(intArrayOf()).contentEquals(intArrayOf()) -> "Fail#IntReference"
        !long(longArrayOf(1, 2, 3)).contentEquals(longArrayOf(1, 2, 3)) -> "Fail#LongReference"
        !float(floatArrayOf(1f)).contentEquals(floatArrayOf(1f)) -> "Fail#FloatReference"
        !double(doubleArrayOf(1.0, 2.0, 3.0)).contentEquals(doubleArrayOf(1.0, 2.0, 3.0)) -> "Fail#DoubleReference"
        !char(charArrayOf('a', 'b')).contentEquals(charArrayOf('a', 'b')) -> "Fail#CharReference"
        !boolean(booleanArrayOf()).contentEquals(booleanArrayOf()) -> "Fail#BooleanReference"

        !adaptedByte(1, 2, 3).contentEquals(byteArrayOf(1, 2, 3)) -> "Fail#AdaptedByte"
        !adaptedShort(1, 2, 3).contentEquals(shortArrayOf(1, 2, 3)) -> "Fail#AdaptedShort"
        !adaptedInt(1, 2, 3).contentEquals(intArrayOf(1, 2, 3)) -> "Fail#AdaptedInt"
        !adaptedLong().contentEquals(longArrayOf()) -> "Fail#AdaptedLong"
        !adaptedFloat(1f).contentEquals(floatArrayOf(1f)) -> "Fail#AdaptedFloat"
        !adaptedDouble().contentEquals(doubleArrayOf()) -> "Fail#AdaptedDouble"
        !adaptedChar('a').contentEquals(charArrayOf('a')) -> "Fail#AdaptedChar"
        !adaptedBoolean(true, false, true).contentEquals(booleanArrayOf(true, false, true)) -> "Fail#AdaptedBoolean"
        else -> "OK"
    }
}
