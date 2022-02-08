// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

import kotlin.test.*

fun box(): String {
    val cs = "1111"
    var sum = 0
    for (i in cs.indices.reversed()) {
        sum = sum * 10 + i + cs[i].toInt() - '0'.toInt()
    }
    assertEquals(4321, sum)

    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// JVM_TEMPLATES
// 1 IFLT
// 1 IF

// JVM_IR_TEMPLATES
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF

// JVM_IR_TEMPLATES
// 7 ILOAD
// 4 ISTORE
// 3 IADD
// 1 ISUB
// 1 IINC