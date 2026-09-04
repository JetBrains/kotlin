// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.map
fun box(): String {
    var list = listOf(1, 2, 3)
    val doubled = list.asSequence().map { it * 2 }.toList()
    if (doubled != listOf(2, 4, 6)) return "Fail: $doubled"

    val reassignedInside = list.asSequence().map { list = listOf(); it }.toList()
    if (reassignedInside != listOf(1, 2, 3)) return "Fail reassigned inside: $reassignedInside"
    return "OK"
}
