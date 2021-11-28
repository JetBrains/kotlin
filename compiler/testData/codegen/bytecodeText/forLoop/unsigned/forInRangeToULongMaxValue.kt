// WITH_STDLIB

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

const val M = ULong.MAX_VALUE

fun f(a: ULong): Int {
    var n = 0
    for (i in a..M) {
        n++
    }
    return n
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 INVOKESTATIC kotlin/UnsignedKt.ulongCompare
// 2 IF
// 0 INVOKESTATIC kotlin/ULong.constructor-impl
// 0 INVOKE\w+ kotlin/ULong.(un)?box-impl

// JVM_IR_TEMPLATES
// 1 ILOAD
// 1 ISTORE
// 0 IADD
// 0 ISUB
// 1 IINC