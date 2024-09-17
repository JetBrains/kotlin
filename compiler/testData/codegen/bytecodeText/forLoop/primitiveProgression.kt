// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun f() {
    for (i in 0..5 step 2) {
    }

    for (i in 5 downTo 1 step 1) {
    }
}

// 0 iterator

// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF_ICMPGT
// 1 IF_ICMPEQ
// 1 IF_ICMPLE
// 3 IF
// 1 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement \(III\)I
// 6 ILOAD
// 4 ISTORE
// 2 IINC
// 0 IADD
// 0 ISUB
