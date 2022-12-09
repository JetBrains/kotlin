// !OPT_IN: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: JVM

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

const val M = Int.MIN_VALUE

fun f(a: Int): Int {
    var n = 0
    for (i in a..<M) {
        n++
    }
    return n
}

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 3 ILOAD
// 2 ISTORE
// 0 IADD
// 0 ISUB
// 2 IINC