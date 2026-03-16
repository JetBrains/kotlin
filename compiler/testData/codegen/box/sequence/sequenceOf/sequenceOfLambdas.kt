// WITH_STDLIB

// CHECK_BYTECODE_TEXT
fun box(): String {
    val k = 2
    val seq = sequenceOf<(Int) -> Int>({ it * k }, { it * 3 }, { it * 4 }).map<(Int) -> Int, Int> { it(3) }
    val list = listOf(6, 9, 12)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: expected: ${list[index - 1]}, but got: $item"
    }
    return "OK"
}
