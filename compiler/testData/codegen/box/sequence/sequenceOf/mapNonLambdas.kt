// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
// 1 TABLESWITCH
fun test(x: Int): Int {
    return x * 2
}

fun testFunctionReference(): Boolean {
    val seq = sequenceOf(1, 2, 3).map(::test)
    val expected = listOf(2, 4, 6)
    var index = 0
    for (item in seq) {
        if (expected[index++] != item) return false
    }
    return true
}

fun box(): String {
    if (!testFunctionReference()) return "failed on function reference test"
    return "OK"
}
