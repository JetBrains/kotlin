// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun x(): Int { calls++; return 3 }
fun box(): String {
    val r = sequenceOf(1, 2, 3).indexOf(x())
    return if (r == 2 && calls == 1) "OK" else "Fail: r=$r calls=$calls"
}
