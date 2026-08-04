// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { it * 2 }
    val list = listOf(2, 4, 6)
    if (seq.toList() != list) return "Failed: Sequence.toList() is ${seq.toList()} while list is $list"
    return "OK"
}