// WITH_STDLIB

fun box(): String {
    var k = 6
    val seq = generateSequence { 1 }.take(k)
    k = 2
    val expected = listOf(1, 1, 1, 1, 1, 1)
    var index = 0
    for (i in seq) {
        if (expected[index++] != i) return "failed: expected ${expected[index - 1]}, got $i"
    }
    if (index != expected.size) return "failed: expected the sequence to have ${expected.size} elements, but had $index elements"
    return "OK"
}
