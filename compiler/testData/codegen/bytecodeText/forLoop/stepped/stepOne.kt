// TARGET_BACKEND: JVM_IR

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for (i in 1..4 step 1) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, when the step is constant and == 1, and "step" is called on a literal progression which we already
// know to have a step whose absolute value is 1, we can essentially ignore the "step" call.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 IF_ICMPLE
// 1 IF

// JVM_IR_TEMPLATES
// 2 ILOAD
// 2 ISTORE
// 0 IADD
// 0 ISUB
// 1 IINC