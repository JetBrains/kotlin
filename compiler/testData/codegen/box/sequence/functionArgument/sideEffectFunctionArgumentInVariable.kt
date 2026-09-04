// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 kotlin/sequences/SequencesKt.map
// a sequence with a function argument evaluated once is not created anew at every place of use
var calls = 0
fun makeF(): (Int) -> Int {
    calls++
    return { it * 2 }
}

fun box(): String {
    val sequence = sequenceOf(1, 2).map(makeF())
    val a = sequence.toList()
    val b = sequence.toList()
    return if (calls == 1 && a == listOf(2, 4) && b == a) "OK" else "Fail: calls=$calls a=$a b=$b"
}
