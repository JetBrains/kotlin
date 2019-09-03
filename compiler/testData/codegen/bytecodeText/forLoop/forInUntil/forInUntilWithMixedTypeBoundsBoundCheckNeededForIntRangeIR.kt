// TARGET_BACKEND: JVM_IR
fun testByteUntilInt(a: Byte, b: Int): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testShortUntilInt(a: Short, b: Int): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

// For "until" progressions in JVM IR, there is typically a check that the range is not empty: upper bound != MIN_VALUE.
// However, this check is not needed when the upper bound is smaller than the range element type.
// Here are the available `until` extension functions with mixed bounds that return IntRange:
//
//   infix fun Byte.until(to: Byte): IntRange
//   infix fun Byte.until(to: Short): IntRange
//   infix fun Byte.until(to: Int): IntRange     // Bound check needed
//   infix fun Short.until(to: Byte): IntRange
//   infix fun Short.until(to: Short): IntRange
//   infix fun Short.until(to: Int): IntRange    // Bound check needed
//   infix fun Int.until(to: Byte): IntRange
//   infix fun Int.until(to: Short): IntRange

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 LDC -2147483648
// 2 IF_ICMPEQ
// 2 IF_ICMPGT
// 2 IF_ICMPLE
// 6 IF