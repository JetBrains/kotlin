// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt
// function arguments with side effects are evaluated once before the iteration, like when the stdlib creates the sequence
var calls = 0
fun <T> once(value: T): T {
    calls++
    return value
}

fun box(): String {
    val map = sequenceOf(1, 2, 3).map(once { x: Int -> x * 2 }).toList()
    if (map != listOf(2, 4, 6)) return "Fail map: $map"
    val mapIndexed = sequenceOf(1, 2, 3).mapIndexed(once { i: Int, x: Int -> i + x }).toList()
    if (mapIndexed != listOf(1, 3, 5)) return "Fail mapIndexed: $mapIndexed"
    val mapNotNull = sequenceOf(1, 2, 3).mapNotNull(once { x: Int -> if (x == 2) null else x }).toList()
    if (mapNotNull != listOf(1, 3)) return "Fail mapNotNull: $mapNotNull"
    val mapIndexedNotNull = sequenceOf(1, 2, 3).mapIndexedNotNull(once { i: Int, x: Int -> if (i == 0) null else x }).toList()
    if (mapIndexedNotNull != listOf(2, 3)) return "Fail mapIndexedNotNull: $mapIndexedNotNull"
    val filter = sequenceOf(1, 2, 3).filter(once { x: Int -> x > 1 }).toList()
    if (filter != listOf(2, 3)) return "Fail filter: $filter"
    val filterNot = sequenceOf(1, 2, 3).filterNot(once { x: Int -> x > 1 }).toList()
    if (filterNot != listOf(1)) return "Fail filterNot: $filterNot"
    val takeWhile = sequenceOf(1, 2, 3).takeWhile(once { x: Int -> x < 3 }).toList()
    if (takeWhile != listOf(1, 2)) return "Fail takeWhile: $takeWhile"
    val dropWhile = sequenceOf(1, 2, 3).dropWhile(once { x: Int -> x < 2 }).toList()
    if (dropWhile != listOf(2, 3)) return "Fail dropWhile: $dropWhile"
    return if (calls == 8) "OK" else "Fail: calls=$calls"
}
