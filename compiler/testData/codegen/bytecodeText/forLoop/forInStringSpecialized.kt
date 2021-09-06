// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun test() {
    var s = ""
    for (c in "testString") {
        s += c
    }
}

// 0 iterator
// 0 hasNext
// 0 nextChar
// 0 INVOKEINTERFACE
// 1 charAt \(I\)C
// 1 length \(\)I

// JVM_IR_TEMPLATES
// 4 ILOAD
// 3 ISTORE
// 0 IADD
// 0 ISUB
// 1 IINC
