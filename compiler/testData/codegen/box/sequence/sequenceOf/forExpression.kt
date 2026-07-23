// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 2 TABLESWITCH

fun box(): String {
    val sequence = sequenceOf(1, 2, 3)
    val list = listOf(1, 2, 3)
    var index = 0
    for (i in sequence)
        if (list[index++] != i) return "Failed: expected ${list[index - 1]}, got $i"
    for (i in sequence)
        Unit
    return "OK"
}
