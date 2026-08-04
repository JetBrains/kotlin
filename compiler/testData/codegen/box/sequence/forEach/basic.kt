// WITH_STDLIB

fun box(): String {
    var result = ""
    sequenceOf(1, 2, 3).map { it + 1 }.filter { it % 2 == 0 }.forEach { if (it == 2) result += "O" else if (it == 4) result += "K" else result += "$it" }
    val list = listOf(1, 2)
    val expected = listOf(5, 7)
    var index = 0
    list.asSequence().map { 2 * it + 3 }.forEach { if (expected[index++] != it) result += "failed: expected ${expected[index - 1]} but was $it" }
    return result
}
