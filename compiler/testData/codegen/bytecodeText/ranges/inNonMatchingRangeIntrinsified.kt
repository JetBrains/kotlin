// IGNORE_BACKEND_FIR: JVM_IR
fun inInt(x: Long): Boolean {
    return x in 1..2
}

fun inLong(x: Int): Boolean {
    return x in 1L..2L
}

fun inFloat(x: Double): Boolean {
    return x in 1.0f..2.0f
}

fun inDouble(x: Float): Boolean {
    return x in 1.0..2.0
}

// 0 INVOKESPECIAL
// 0 NEW
// 0 rangeTo
// 0 longRangeContains
// 0 intRangeContains
// 0 doubleRangeContains
// 0 floatRangeContains
// 0 contains

// JVM_TEMPLATES
// 2 I2L
// 3 F2D

// JVM_IR_TEMPLATES
// 1 I2L
// 1 F2D