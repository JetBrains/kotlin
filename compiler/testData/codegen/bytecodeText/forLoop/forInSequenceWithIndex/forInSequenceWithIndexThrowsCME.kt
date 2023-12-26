// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-63864

// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

val xsl = arrayListOf("a", "b", "c", "d")
val xs = xsl.asSequence()

fun box(): String {
    val s = StringBuilder()

    var cmeThrown = false
    try {
        for ((index, x) in xs.withIndex()) {
            s.append("$index:$x;")
            xsl.clear()
        }
    } catch (e: java.util.ConcurrentModificationException) {
        cmeThrown = true
    }

    if (!cmeThrown) return "Fail: CME should be thrown"

    val ss = s.toString()
    return if (ss == "0:a;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// The 1st ICONST_0 is for initializing the list. 2nd is for cmeThrown. 3rd is for initializing the index in the lowered for-loop.
// 3 ICONST_0

// JVM_IR_TEMPLATES
// 4 ILOAD
// 5 ISTORE
// 1 IADD
// 0 ISUB
// 0 IINC