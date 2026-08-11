// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    var acc = 0
    for (x in sequence { yield(1); yield(2) }) for (y in sequenceOf(10, 20)) acc += x * y
    val list = mutableListOf<Int>()
    for (x in sequence { yieldAll(listOf(1, 2)) }.map { it * 2 }) {
        for (y in sequence { yieldAll(listOf(3, 4)) }) list += x * y
    }
    return if (acc == 90 && list == listOf(6, 8, 12, 16)) "OK" else "Fail: $acc, $list"
}
