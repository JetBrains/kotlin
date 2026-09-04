// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.takeWhile
class Box(val value: Int)

fun box(): String {
    val threshold = 1
    val filtered = sequenceOf(1, 2, 3).filter { it > threshold }.toList()
    if (filtered != listOf(2, 3)) return "Fail filter: $filtered"

    val limit = Box(3)
    val taken = sequenceOf(1, 2, 3, 4).takeWhile { it < limit.value }.toList()
    if (taken != listOf(1, 2)) return "Fail takeWhile: $taken"

    val withLocalVar = sequenceOf(1, 2, 3).filter { var x = it; x += threshold; x > 2 }.toList()
    if (withLocalVar != listOf(2, 3)) return "Fail local var: $withLocalVar"
    return "OK"
}
