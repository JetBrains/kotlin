// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun box(): String {
    for (iv in (4..7).withIndex()) {
    }

    return "OK"
}

// We do not optimize `withIndex()` if the loop variable is not destructured

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// JVM_IR_TEMPLATES
// 0 ILOAD
// 0 ISTORE
// 0 IADD
// 0 ISUB
// 0 IINC