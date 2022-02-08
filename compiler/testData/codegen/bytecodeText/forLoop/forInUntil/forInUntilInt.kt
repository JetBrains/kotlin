// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun test(a: Int, b: Int): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 6 ILOAD
// 3 ISTORE
// 1 IADD
// 0 ISUB
// 1 IINC