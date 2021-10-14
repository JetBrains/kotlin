// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun intArray() = intArrayOf(0, 0, 0, 0)
fun longArray() = longArrayOf(0, 0, 0, 0)

fun f(): Int {
    var n = 0
    for (i in intArray()) {
        n++
    }

    for (i in longArray()) {
        n++
    }
    return n
}

// 0 iterator
// 1 INVOKESTATIC .*\.intArray \(\)
// 1 INVOKESTATIC .*\.longArray \(\)
// 2 ARRAYLENGTH
// 2 IF_ICMPGE
// 2 IF

// JVM_IR_TEMPLATES
// 7 ILOAD
// 6 ISTORE
// 0 IADD
// 0 ISUB
// 4 IINC