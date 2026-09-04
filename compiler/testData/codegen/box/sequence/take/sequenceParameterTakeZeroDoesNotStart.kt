// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.take
// 0 kotlin/sequences/SequencesKt.toList

var calls = 0
fun takeNone(s: Sequence<Int>): List<Int> = s.take(0).toList()
fun box(): String {
    val r = takeNone(sequence { calls++; yield(1) })
    return if (r.isEmpty() && calls == 0) "OK" else "Fail: $r, calls=$calls"
}
