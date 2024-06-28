// FIR_IDENTICAL
// WITH_STDLIB

typealias MyByte = Byte
typealias MyShort = Short
typealias MyInt = Int
typealias MyLong = Long

typealias MyUByte = UByte
typealias MyUShort = UShort
typealias MyUInt = UInt
typealias MyULong = ULong

typealias MyFloat = Float
typealias MyDouble = Double

typealias MyChar = Char
typealias MyBoolean = Boolean

fun checkByte(vararg values: Byte) = Unit
fun checkShort(vararg values: Short) = Unit
fun checkInt(vararg values: Int) = Unit
fun checkLong(vararg values: Long) = Unit

fun checkMyByte(vararg values: MyByte) = Unit
fun checkMyShort(vararg values: MyShort) = Unit
fun checkMyInt(vararg values: MyInt) = Unit
fun checkMyLong(vararg values: MyLong) = Unit

fun checkUByte(vararg values: UByte) = Unit
fun checkUShort(vararg values: UShort) = Unit
fun checkUInt(vararg values: UInt) = Unit
fun checkULong(vararg values: ULong) = Unit

fun checkMyUByte(vararg values: MyUByte) = Unit
fun checkMyUShort(vararg values: MyUShort) = Unit
fun checkMyUInt(vararg values: MyUInt) = Unit
fun checkMyULong(vararg values: MyULong) = Unit

fun checkFloat(vararg values: Float) = Unit
fun checkDouble(vararg values: Double) = Unit

fun checkMyFloat(vararg values: MyFloat) = Unit
fun checkMyDouble(vararg values: MyDouble) = Unit

fun checkChar(vararg values: Char) = Unit
fun checkBoolean(vararg values: Boolean) = Unit

fun checkMyChar(vararg values: MyChar) = Unit
fun checkMyBoolean(vararg values: MyBoolean) = Unit
