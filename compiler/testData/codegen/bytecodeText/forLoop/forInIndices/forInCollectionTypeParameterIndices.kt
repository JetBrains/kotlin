// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun <T : Collection<*>> test(c: T) {
    var sum = 0
    for (i in c.indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 INVOKEINTERFACE java/util/Collection\.size \(\)I

// 1 IF_ICMPGE
// 0 IF_ICMPGT
// 0 IF_ICMPLE
// 1 IF

// JVM_IR_TEMPLATES
// 4 ILOAD
// 4 ISTORE
// 1 IADD
// 0 ISUB
// 1 IINC
