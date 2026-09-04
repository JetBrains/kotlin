// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun pred(): (Int) -> Boolean { calls++; return { it > 1 } }
fun box(): String {
    val r = sequenceOf(1, 2, 3).filter(pred()).toList()
    return if (calls == 1 && r == listOf(2, 3)) "OK" else "Fail: calls=$calls r=$r"
}
