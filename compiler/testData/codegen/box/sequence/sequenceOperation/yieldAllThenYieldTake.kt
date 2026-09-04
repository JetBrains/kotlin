// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val l = mutableListOf(1, 2)
    val r = sequence { yieldAll(l); yield(3) }.take(1).toList()
    val r2 = sequence { yieldAll(sequenceOf(1, 2)); yield(3) }.take(1).toList()
    val r3 = sequence { yieldAll(l.iterator()); yield(3) }.take(1).toList()
    return if (r == listOf(1) && r2 == listOf(1) && r3 == listOf(1)) "OK" else "Fail: $r $r2 $r3"
}
