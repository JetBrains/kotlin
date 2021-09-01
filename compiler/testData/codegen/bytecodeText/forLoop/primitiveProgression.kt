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

    // JVM non-IR: `step 1` suppresses optimized code generation for 'for-in-downTo'
    // JVM IR: No getProgressionLastElement() call required for `step 1`, equivalent to `5 downTo 1`
    for (i in 5 downTo 1 step 1) {
    }
}

// JVM non-IR does NOT specifically handle "step" progressions. The stepped progressions in the above code are constructed and their
// first/last/step properties are retrieved.
// JVM IR has an optimized handler for "step" progressions and elides the construction of the stepped progressions.

// 0 iterator

// JVM_TEMPLATES
// 2 getFirst
// 2 getLast
// 2 getStep

// JVM_IR_TEMPLATES
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