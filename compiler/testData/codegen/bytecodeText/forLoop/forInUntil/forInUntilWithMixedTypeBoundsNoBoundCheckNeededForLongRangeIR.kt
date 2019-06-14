// TARGET_BACKEND: JVM_IR
fun testLongUntilByte(a: Long, b: Byte): Long {
    var sum = 0L
    for (i in a until b) {
        sum = sum * 10L + i
    }
    return sum
}

fun testLongUntilShort(a: Long, b: Short): Long {
    var sum = 0L
    for (i in a until b) {
        sum = sum * 10L + i
    }
    return sum
}

fun testLongUntilInt(a: Long, b: Int): Long {
    var sum = 0L
    for (i in a until b) {
        sum = sum * 10L + i
    }
    return sum
}

// For "until" progressions in JVM IR, there is typically a check that the range is not empty: upper bound != MIN_VALUE.
// However, this check is not needed when the upper bound is smaller than the range element type.
// Here are the available `until` extension functions with mixed bounds that return LongRange:
//
//   infix fun Long.until(to: Byte): LongRange   // NO bound check needed
//   infix fun Long.until(to: Short): LongRange  // NO bound check needed
//   infix fun Long.until(to: Int): LongRange    // NO bound check needed

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 LDC -9223372036854775808
// 6 LCMP
// 3 IFGT
// 3 IFLE
// 6 IF