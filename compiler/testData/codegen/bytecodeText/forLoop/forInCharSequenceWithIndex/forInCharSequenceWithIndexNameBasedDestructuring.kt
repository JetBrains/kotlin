// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

val cs: CharSequence = "abcd"

fun box(): String {
    val s = StringBuilder()

    for ((i = index, v = value, i2 = index, v2 = value) in cs.withIndex()) {
        s.append("$i:$v:$i2:$v2;")
    }

    val ss = s.toString()
    return if (ss == "0:a:0:a;1:b:1:b;2:c:2:c;3:d:3:d;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 length
// 1 charAt

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0

// 9 ILOAD
// 5 ISTORE
// 0 IADD
// 0 ISUB
// 1 IINC
