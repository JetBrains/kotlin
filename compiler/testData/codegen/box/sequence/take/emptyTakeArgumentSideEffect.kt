// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun n(): Int { calls++; return 1 }
fun box(): String {
    emptySequence<Int>().take(n()).forEach { }
    return if (calls == 1) "OK" else "Fail: calls=$calls"
}
