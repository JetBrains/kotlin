// WITH_STDLIB

var global = 0
fun side_effect(): Int = global++
// CHECK_BYTECODE_TEXT
// 0 iterator
// 2 TABLESWITCH
fun box(): String {
    var factor = 2
    val seq = sequenceOf(1, 2, 3).map { it * factor }

    val expected = listOf(2, 0, 0)
    var index = 0
    for (x in seq) {
        if (expected[index++] != x) return "failed: expected ${expected[index - 1]}, got $x"
        factor = 0
    }

    val expected2 = listOf(1, 3, 5)
    index = 0
    sequenceOf(3, 2, 3).map { side_effect() }.map { side_effect() }.forEach { x ->
        if (expected2[index++] != x) return "failed: expected ${expected2[index - 1]}, got $x"
    }
    return "OK"
}
