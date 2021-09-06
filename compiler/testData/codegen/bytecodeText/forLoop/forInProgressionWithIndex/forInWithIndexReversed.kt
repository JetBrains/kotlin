// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for ((i, v) in (4..7).withIndex().reversed()) {
    }

    return "OK"
}

// We do not optimize `withIndex().reversed()`

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 1 component1
// 1 component2
// 1 reversed

// JVM_IR_TEMPLATES
// 0 ILOAD
// 2 ISTORE
// 0 IADD
// 0 ISUB
// 0 IINC