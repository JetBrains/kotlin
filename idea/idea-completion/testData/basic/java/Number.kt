// FIR_COMPARISON
fun test(x: java.lang.Integer) {
    x.<caret>
}

// EXIST: toByte
// EXIST: toShort
// EXIST: toInt
// EXIST: toLong
// EXIST: toFloat
// EXIST: toDouble
