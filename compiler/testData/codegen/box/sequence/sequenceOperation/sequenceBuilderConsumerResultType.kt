// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val list = sequence { yield(1); yield(2) }.toCollection(ArrayList()).toString()
    val last = sequence { yield(1); yield(2) }.findLast { it > 5 }.toString()
    val firstNotNull = sequence { yield(1) }.firstNotNullOfOrNull { if (it > 1) it else null }.toString()
    val r = "$list $last $firstNotNull"
    return if (r == "[1, 2] null null") "OK" else "Fail: $r"
}
