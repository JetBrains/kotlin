// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun <T : Iterable<*>> test(iterable: T): String {
    val s = StringBuilder()

    for ((index, x) in iterable.withIndex()) {
        s.append("$index:$x;")
    }

    return s.toString()
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// JVM_IR_TEMPLATES
// 3 ILOAD
// 3 ISTORE
// 1 IADD
// 0 ISUB
// 0 IINC
