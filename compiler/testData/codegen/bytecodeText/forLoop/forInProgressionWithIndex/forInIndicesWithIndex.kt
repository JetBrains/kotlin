// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for ((i, v) in listOf(4, 5, 6, 7).indices.withIndex()) {
    }

    return "OK"
}

// 0 withIndex
// 0 component1
// 0 component2

// The 1st ICONST_0 is for initializing the list. 2nd is for initializing the index in the lowered for-loop.
// 2 ICONST_0

// JVM_TEMPLATES
// 1 iterator
// 1 hasNext
// 1 next

// JVM_IR_TEMPLATES
// 0 iterator
// 0 hasNext
// 0 next
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 getIndices

// JVM_IR_TEMPLATES
// 4 ILOAD
// 4 ISTORE
// 0 IADD
// 0 ISUB
// 1 IINC