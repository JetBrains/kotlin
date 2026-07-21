// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 sequenceOf

fun box(): String {
    val expected = listOf(2, 4, 6)
    var index = 0
    sequenceOf(1, 2, 3).map { x ->
        sequenceOf(x, 2).map { it * 2 }.first()
    }.forEach{
        if (it != expected[index++]) return "failed: expected ${expected[index - 1]}, actual $it"
    }
    return "OK"
}
