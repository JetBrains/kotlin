// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for (i in 1u..6u step 0) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, if the step is constant and <= 0, the expression for step is replaced with an
// IllegalArgumentException. The backend can then eliminate the entire loop and the rest of the function as dead code.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 1 NEW java/lang/IllegalArgumentException
// 1 ATHROW
// 0 IF
// 0 ARETURN
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl

// JVM_IR_TEMPLATES
// 0 ILOAD
// 0 ISTORE
// 0 IADD
// 0 ISUB
// 0 IINC