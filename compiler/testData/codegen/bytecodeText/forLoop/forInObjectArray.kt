// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun array() = arrayOfNulls<Any>(4)

fun f(): Int {
    var n = 0
    for (i in array()) {
        n++
    }
    return n
}

// 0 iterator
// 1 INVOKESTATIC .*\.array \(\)
// 1 ARRAYLENGTH
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 4 ILOAD
// 3 ISTORE
// 0 IADD
// 0 ISUB
// 2 IINC