// WITH_STDLIB
// CHECK_BYTECODE_TEXT

fun testMaps(): Boolean {
    val seq = sequenceOf(1, 2, 3).map { it * 3 }
    val seq2 = seq.map { it + 1 }
    val expected = listOf(4, 7, 10)
    var index = 0
    for (item in seq2) {
        if (expected[index++] != item) return false
    }
    val seq3 = sequenceOf(1, 2, 3)
    val expected2 = listOf(5, 6, 7)
    index = 0
    for (item in seq3.map { it + 4 }) {
        if (expected2[index++] != item) return false
    }
    return true
}

fun basicTest(): Boolean {
    val seq = sequenceOf(1, 2, 3)
    val list = listOf(1, 2, 3)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return false
    }
    index = 0
    for (item in sequenceOf(1, 2, 3)) {
        if (item != list[index++]) return false
    }
    return true
}

fun testFilters(): Boolean {
    val seq = sequenceOf(1, 2, 3, 4).map { it * 2 }.filter { it % 4 == 0 }.map { it / 2 }.filter { it != 2 }.map { it - 3 }
    val expected = listOf(1)
    var index = 0
    for (item in seq) {
        if (item != expected[index++]) return false
    }
    return true
}

fun box(): String {
    if(!basicTest()) return "failed: basic test failed"
    if(!testMaps()) return "failed: basic map test failed"
    if(!testFilters()) return "failed: basic filter test failed"
    return "OK"
}
