// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seq = generateSequence(1) { if (it > 9) null else it + 1 }
    val list = listOf(2, 6, 10, 14, 18)
    var index = 0
    for (item in seq.map { it * 2 }.filter { it % 4 != 0 }) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item, for seq"
    }
    val seq2 = generateSequence ({ 1 }, { x: Int -> if (x > 9) null else x + 1 }).map { it * 2 }
    index = 0
    for (item in seq2.filter { it % 4 != 0 }) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item, for seq2"
    }
    var k = 1
    val seq3 = generateSequence { if (k > 9) null else k++ }.map { it * 2 }.filter { it % 4 != 0 }
    index = 0
    for (item in seq3) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item, for seq3"
    }
    return "OK"
}
