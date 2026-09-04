// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.toList
fun box(): String {
    val list = emptySequence<String>().toList()
    if (list.isNotEmpty()) return "Fail: $list"
    return "OK"
}
