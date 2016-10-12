// WITH_RUNTIME

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

// 2 INVOKESPECIAL
// 2 NEW
// 2 INVOKESTATIC kotlin/ranges/RangesKt.rangeTo
// 1 longRangeContains
// 1 intRangeContains
// 1 doubleRangeContains
// 1 floatRangeContains
