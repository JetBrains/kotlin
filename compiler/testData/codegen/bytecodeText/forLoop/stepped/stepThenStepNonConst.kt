// TARGET_BACKEND: JVM_IR

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun one() = 1

fun box(): String {
    for (i in 1..6 step one() step one()) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, if the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException
// is thrown.
//

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 2 NEW java/lang/IllegalArgumentException
// 2 ATHROW
// 1 IF_ICMPGT
// 1 IF_ICMPEQ
// 2 IFGT
// 4 IF
// 14 ILOAD
// 7 ISTORE
// 1 IADD
// 0 ISUB
// 0 IINC
