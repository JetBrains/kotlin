// WITH_RUNTIME

fun Byte.in2(left: Byte, right: Byte): Boolean {
    return this in left..right
}

fun inInt(x: Int, left: Int, right: Int): Boolean {
    return x in left..right
}

fun inDouble(x: Double, left: Double, right: Double): Boolean {
    return x in left..right
}

fun inFloat(x: Float, left: Float, right: Float): Boolean {
    return x in left..right
}

fun inLong(x: Long, left: Long, right: Long): Boolean {
    return x in left..right
}

fun inCharWithNullableParameter(x: Char?, left: Char, right: Char): Boolean {
    return x!! in left..right
}

// 0 INVOKESPECIAL
// 0 NEW
// 1 INVOKEVIRTUAL java/lang/Character.charValue
// 1 INVOKEVIRTUAL
// 0 CHECKCAST
// 0 INVOKEINTERFACE
// 0 <init>
