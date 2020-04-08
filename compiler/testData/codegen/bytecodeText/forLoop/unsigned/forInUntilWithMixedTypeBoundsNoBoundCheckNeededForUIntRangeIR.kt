// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun testUByteUntilUByte(a: UByte, b: UByte): UInt {
    var sum = 0u
    for (i in a until b) {
        sum = sum * 10u + i
    }
    return sum
}

fun testUShortUntilUShort(a: UShort, b: UShort): UInt {
    var sum = 0u
    for (i in a until b) {
        sum = sum * 10u + i
    }
    return sum
}

// For "until" progressions in JVM IR, there is typically a check that the range is not empty: upper bound != MIN_VALUE.
// However, this check is not needed when the upper bound is smaller than the range element type.
// Here are the available `until` extension functions with mixed bounds that return UIntRange:
//
//   infix fun UByte.until(to: UByte): UIntRange    // NO bound check needed
//   infix fun UShort.until(to: UShort): UIntRange  // NO bound check needed

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 IFGT
// 2 IFLE
// 4 IF