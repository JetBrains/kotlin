// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

const val N = 42L

fun test(): Long {
    var sum = 0L
    for (i in 1L .. N) {
        sum += i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// 0 L2I
// 0 I2L

// JVM_TEMPLATES
// 1 IFGT
// 1 LCMP
// 1 IF

// JVM_IR_TEMPLATES
// 1 LCMP
// 1 IFGE
// 1 IF

// JVM_IR_TEMPLATES
// 0 ILOAD
// 0 ISTORE
// 0 IADD
// 0 ISUB
// 0 IINC