// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
fun make(): List<Int> { calls++; return listOf(1, 2) }
fun box(): String {
    val s = make().asSequence()
    var sum = 0
    for (x in s) sum += x
    for (x in s) sum += x
    return if (calls == 1 && sum == 6) "OK" else "Fail: calls=$calls sum=$sum"
}
