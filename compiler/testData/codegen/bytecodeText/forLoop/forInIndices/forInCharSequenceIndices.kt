// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun test(s: CharSequence): Int {
    var result = 0
    for (i in s.indices) {
        result = result * 10 + (i + 1)
    }
    return result
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast

// 0 IF_ICMPGT
// 0 IF_ICMPEQ
// 0 IF_ICMPLE
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 5 ILOAD
// 4 ISTORE
// 2 IADD
// 0 ISUB
// 1 IINC