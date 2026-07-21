// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 asSequence
fun box(): String {
    var index = 0
    val expected = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    (1..10).asSequence().forEach { if (it != expected[index++]) return "fail: $it != ${expected[index]}" }
    return "OK"
}
