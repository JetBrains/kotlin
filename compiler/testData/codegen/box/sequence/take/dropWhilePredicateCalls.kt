// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    var calls = 0
    val r = sequenceOf(1, 2, 1).dropWhile { calls++; it < 2 }.toList()
    return if (r == listOf(2, 1) && calls == 2) "OK" else "Fail: r=$r calls=$calls"
}
