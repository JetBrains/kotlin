// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for ((outer, iv) in (4..7).withIndex().withIndex()) {
    }

    return "OK"
}

// We optimize the outer `withIndex()` and treat the inner one as an Iterable

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0

// JVM_IR_TEMPLATES
// 2 ILOAD
// 3 ISTORE
// 1 IADD
// 0 ISUB
// 0 IINC