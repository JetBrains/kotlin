// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun cond(): Boolean { calls++; return true }
fun box(): String {
    val double: (Int) -> Int = { it * 2 }
    val triple: (Int) -> Int = { it * 3 }
    val r = sequenceOf(1, 2, 3).map(if (cond()) double else triple).toList()
    return if (r == listOf(2, 4, 6) && calls == 1) "OK" else "Fail: $r, calls=$calls"
}
