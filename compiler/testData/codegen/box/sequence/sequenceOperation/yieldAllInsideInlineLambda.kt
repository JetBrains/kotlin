// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val fromSequence = sequence {
        repeat(2) { yieldAll(sequenceOf(1, 2)) }
    }.toList()
    if (fromSequence != listOf(1, 2, 1, 2)) return "Fail sequence: $fromSequence"

    val list = listOf(3, 4)
    val fromIterable = sequence {
        listOf(1, 2).forEach { yieldAll(list) }
    }.toList()
    if (fromIterable != listOf(3, 4, 3, 4)) return "Fail iterable: $fromIterable"

    val fromIterator = sequence {
        repeat(2) { yieldAll(list.iterator()) }
    }.take(3).toList()
    if (fromIterator != listOf(3, 4, 3)) return "Fail iterator: $fromIterator"

    val stopped = sequence {
        repeat(3) { i -> yieldAll(generateSequence(i * 10) { if (it < i * 10 + 1) it + 1 else null }) }
    }.first { it == 11 }
    return if (stopped == 11) "OK" else "Fail stopped: $stopped"
}
