// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun box(): String {
    val r = generateSequence { calls++; 1 }.take(0).toList()
    return if (r.isEmpty() && calls == 0) "OK" else "Fail: $r, calls=$calls"
}
