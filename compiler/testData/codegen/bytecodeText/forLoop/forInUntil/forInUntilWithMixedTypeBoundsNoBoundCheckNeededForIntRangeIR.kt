// TARGET_BACKEND: JVM_IR
fun testByteUntilByte(a: Byte, b: Byte): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testByteUntilShort(a: Byte, b: Short): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testShortUntilByte(a: Short, b: Byte): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testShortUntilShort(a: Short, b: Short): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testIntUntilByte(a: Int, b: Byte): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

fun testIntUntilShort(a: Int, b: Short): Int {
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
//   infix fun Byte.until(to: Byte): IntRange    // NO bound check needed
//   infix fun Byte.until(to: Short): IntRange   // NO bound check needed
//   infix fun Byte.until(to: Int): IntRange
//   infix fun Short.until(to: Byte): IntRange   // NO bound check needed
//   infix fun Short.until(to: Short): IntRange  // NO bound check needed
//   infix fun Short.until(to: Int): IntRange
//   infix fun Int.until(to: Byte): IntRange     // NO bound check needed
//   infix fun Int.until(to: Short): IntRange    // NO bound check needed

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 LDC -2147483648
// 6 IF_ICMPGT
// 6 IF_ICMPLE
// 12 IF