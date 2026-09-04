// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun pred(): (Int) -> Boolean { calls++; return { it < 3 } }
fun box(): String {
    val r = sequenceOf(1, 2, 3).takeWhile(pred()).toList()
    return if (calls == 1 && r == listOf(1, 2)) "OK" else "Fail: calls=$calls r=$r"
}
