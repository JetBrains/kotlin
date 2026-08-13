// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
// 1 TABLESWITCH
fun box(): String {
    val seq = sequenceOf(12, 23, 34, "cake").map(
        { x ->
            when (x) {
                is Int -> x + 2
                is String -> x + "2"
                else -> x
            }
        })
    val list = listOf(14, 25, 36, "cake2")
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
