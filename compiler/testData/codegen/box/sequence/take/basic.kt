// WITH_STDLIB

fun box(): String {
    val seq = listOf(1, 2, 3).asSequence().map { it * 2 }.take(3).take(2)
    val expected = listOf(2, 4)
    var index = 0
    for (i in seq) {
        if (expected[index++] != i ) return "failed: expected ${expected[index - 1]}, but got $i"
    }
    if (index != expected.size) return "failed: expected ${expected.size} elements, but got $index"
    var k = 1
    val seq2 = generateSequence { k++ }.map { it * 2 }.filter { it % 4 == 0 }.take(5).take(4)
    val expected2 = listOf(4, 8, 12, 16)
    index = 0
    for (i in seq2) {
        if (expected2[index++] != i ) return "failed: expected ${expected2[index - 1]}, but got $i"
    }
    if (index != expected2.size) return "failed: expected ${expected2.size} elements, but got $index"
    val seq3 = generateSequence(1) { x -> x + 1 }.map { it + 1 }.take(7).filter { it != 3 }.take(4).take(3).take(2).take(1)
    val expected3 = listOf(2)
    index = 0
    for (i in seq3) {
        if (expected3[index++] != i) return "failed: expected ${expected3[index - 1]}, but got $i"
    }
    if (index != expected3.size) return "failed: expected ${expected3.size} elements, but got $index"
    val seq4 = sequenceOf(7, 8, 9, 0).take(3).map { 1 / it }.take(2)
    val expected4 = listOf(0, 0)
    index = 0
    for (i in seq4) {
        if (expected4[index++] != i) return "failed: expected ${expected4[index - 1]}, but got $i"
    }
    return "OK"
}

