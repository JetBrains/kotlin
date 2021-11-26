// WITH_STDLIB

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun test(uis: UIntArray): UInt {
    var s = 0U
    for ((i, ui) in uis.withIndex()) {
        s += ui
    }
    return s
}

// JVM_TEMPLATES
// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 1 component1
// 1 component2
// 0 ARRAYLENGTH
// 1 ICONST_0

// JVM_IR_TEMPLATES
// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 INVOKESTATIC kotlin\/UIntArray\.getSize\-impl \(\[I\)I
// 2 ICONST_0

// JVM_IR_TEMPLATES
// 7 ILOAD
// 6 ISTORE
// 1 IADD
// 0 ISUB
// 1 IINC