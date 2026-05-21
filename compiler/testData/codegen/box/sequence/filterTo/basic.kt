// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 filterTo
// 0 filterNotTo
// 0 filterNotNullTo

fun box(): String {
    val seq = sequenceOf(1, 2, 3, 4)
    val list = mutableListOf<Int>()
    seq.filterTo(list) { it % 2 == 0 }
    if (list != listOf(2, 4)) return "failed FilterTo, got $list"
    val list2 = mutableListOf<Int>()
    generateSequence(1) { if (it < 4) it + 1 else null }.filterNotTo(list2) { it % 2 == 0 }
    if (list2 != listOf(1, 3)) return "failed FilterNotTo, got $list2"
    val seq3 = listOf(-1, -2, null, -3, -4, null).asSequence().map { it?.times(-1) }
    val list3 = mutableListOf<Int>()
    val list4 = seq3.filterNotNullTo(list3)
    if (list3 != listOf(1, 2, 3, 4)) return "failed FilterNotNullTo, got $list3"
    if (list4 != listOf(1, 2, 3, 4)) return "failed: FilterNotNullTo returned $list4"
    return "OK"
}
