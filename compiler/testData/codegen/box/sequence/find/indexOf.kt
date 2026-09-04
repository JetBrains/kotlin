// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 sequenceOf
fun box(): String {
    val index = sequenceOf(2, 3, 4).indexOf(3)
    if (index != 1) return "fail: expected 1, got $index"
    val index2 = sequenceOf(3, 5, 6, 7, 8).indexOfFirst { it % 2 == 0 }
    if (index2 != 2) return "fail: expected 2, got $index2"
    val index3 = sequenceOf(3, 5, 6, 7, 8, 9).indexOfLast { it % 2 == 0 }
    if (index3 != 4) return "fail: expected 4, got $index3"
    return "OK"
}
