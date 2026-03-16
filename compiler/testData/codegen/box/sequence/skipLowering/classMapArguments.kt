// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 3 iterator

class TestClass() {
    fun mapMethod(value: Int): Int = value * 2
    val mapProperty: (Int) -> Int = { it * 3 }
}
var EXPENSIVE_OPERATION_COUNTER = 0
fun makeMap(): (Int) -> Int {
    EXPENSIVE_OPERATION_COUNTER++
    return { it * 4 }
}

fun box(): String {
    val test = TestClass()
    val seq = sequenceOf(1, 2, 3).map(test::mapMethod)
    val expected = listOf(2, 4, 6)
    var index = 0
    for (item in seq) {
        if (item != expected[index++]) return "failed: expected ${expected[index - 1]}, but got $item"
    }
    val seq2 = sequenceOf(1, 2, 3).map(test.mapProperty)
    val expected2 = listOf(3, 6, 9)
    index = 0
    for (item in seq2) {
        if (item != expected2[index++]) return "failed: expected ${expected2[index - 1]}, but got $item"
    }
    val seq3 = sequenceOf(1, 2, 3).map(makeMap())
    val expected3 = listOf(4, 8, 12)
    index = 0
    for (item in seq3) {
        if (item != expected3[index++]) return "failed: expected ${expected3[index - 1]}, but got $item"
    }
    if (EXPENSIVE_OPERATION_COUNTER != 1) return "failed: called makeMap() $EXPENSIVE_OPERATION_COUNTER times"
    return "OK"
}