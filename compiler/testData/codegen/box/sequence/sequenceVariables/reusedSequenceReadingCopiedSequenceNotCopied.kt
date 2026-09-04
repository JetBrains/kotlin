// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// `a` is copied into the lambda of `b`, so `b`, which is read twice, is not copied, but `c`, which reads `b`, is copied again
// 1 kotlin/sequences/SequencesKt.sequenceOf
// 2 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList

fun box(): String {
    val a = sequenceOf(1, 2)
    val b = a.map { it + a.first() }
    val c = b.map { it + b.first() }
    val first = c.toList()
    val second = c.toList()
    return if (first == listOf(4, 5) && second == listOf(4, 5)) "OK" else "Fail: $first $second"
}
