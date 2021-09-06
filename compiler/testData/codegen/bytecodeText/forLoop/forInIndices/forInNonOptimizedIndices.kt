// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

import kotlin.test.assertEquals

fun test(coll: Collection<*>?): Int {
    var sum = 0
    for (i in coll?.indices ?: return 0) {
        sum += i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getIndices
// 1 getFirst
// 1 getLast

// JVM_IR_TEMPLATES
// 7 ILOAD
// 4 ISTORE
// 1 IADD
// 0 ISUB
// 1 IINC