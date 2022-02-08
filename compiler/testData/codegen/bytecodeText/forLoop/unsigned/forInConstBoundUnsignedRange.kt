// WITH_STDLIB

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun testUIntRangeLiteral(a: UInt): Int {
    var s = 0
    for (x in a .. 10u) {
        s += x.toInt()
    }
    return s
}

fun testULongRangeLiteral(a: ULong): Int {
    var s = 0
    for (x in a .. 10u) {
        s += x.toInt()
    }
    return s
}

fun testUIntUntil(a: UInt): Int {
    var s = 0
    for (x in a until 10u) {
        s += x.toInt()
    }
    return s
}

fun testULongUntil(a: ULong): Int {
    var s = 0
    for (x in a until 10u) {
        s += x.toInt()
    }
    return s
}

fun testUIntDownTo(a: UInt): Int {
    var s = 0
    for (x in a downTo 10u) {
        s += x.toInt()
    }
    return s
}

fun testULongDownTo(a: ULong): Int {
    var s = 0
    for (x in a downTo 10u) {
        s += x.toInt()
    }
    return s
}

// 0 iterator
// 0 hasNext
// 0 next
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/U(Int|Long).constructor-impl
// 0 INVOKE\w+ kotlin/U(Int|Long).(un)?box-impl

// JVM_IR_TEMPLATES
// 31 ILOAD
// 21 ISTORE
// 6 IADD
// 0 ISUB
// 3 IINC
