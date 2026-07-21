// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.last
var counter = 3

fun getValue(): Int = counter--

fun box(): String {
    val last = sequenceOf(1, 2, 3, 4, 5).take(getValue()).last()
    if (last != 3) return "failed: expected 3, got $last"
    if (counter != 2) return "failed: expected 2, got $counter"
    return "OK"
}
