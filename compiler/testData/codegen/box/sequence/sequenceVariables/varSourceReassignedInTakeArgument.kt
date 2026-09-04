// WITH_STDLIB
fun box(): String {
    var s = sequenceOf(1, 2, 3)
    val taken = s.take(run { s = sequenceOf(7, 8, 9); 2 }).toList()
    var t = sequenceOf(1, 2, 3)
    val collected = t.toCollection(run { t = sequenceOf(7, 8, 9); mutableListOf() })
    return if (taken == listOf(1, 2) && collected == listOf(1, 2, 3)) "OK" else "Fail: $taken, $collected"
}
