// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    data class Test (val mapped: (Int) -> Int) {}
    val test = Test({ it * 2 })
    val sequence = sequenceOf(1, 2, 3).map(test.mapped)
    val list = listOf(2, 4, 6)
    var index = 0
    for (item in sequence) {
        if (list[index++] != item) return "FAIL: Expected ${list[index]}, got $item"
    }
    return "OK"
}