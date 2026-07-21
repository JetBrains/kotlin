// WITH_STDLIB

fun box(): String {
    val fib = generateSequence(1 to 1, { pair -> pair.first + pair.second to pair.first })
    val fibNums = fib.map { it.first }
    val expected = listOf(1, 2, 3, 5, 8, 13, 21, 34, 55, 89)
    var index = 0
    for (num in fibNums) {
        if (index == 9) return "OK"
        if (expected[index++] != num) return "failed: expected: ${expected[index - 1]} but got: $num"
    }
    return "failed: iteration of an infinite sequence terminated"
}
