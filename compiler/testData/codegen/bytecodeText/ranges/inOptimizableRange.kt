// IGNORE_BACKEND: JVM_IR
fun Byte.inByte(left: Byte, right: Byte) = this in left..right

fun Short.inInt(left: Int, right: Int) = this in left .. right

fun Short.inByte(left: Byte, right: Byte) = this in left..right

fun inInt(x: Int, left: Int, right: Int) = x in left..right

fun inDouble(x: Double, left: Double, right: Double) = x in left..right

fun inFloat(x: Float, left: Float, right: Float) = x in left..right

fun inLong(x: Long, left: Long, right: Long) = x in left..right

fun inCharWithNullableParameter(x: Char?, left: Char, right: Char) = x!! in left..right

// 0 INVOKESPECIAL
// 0 NEW
// 1 charValue
// 1 INVOKEVIRTUAL
// 0 CHECKCAST
// 0 INVOKEINTERFACE
// 0 <init>
