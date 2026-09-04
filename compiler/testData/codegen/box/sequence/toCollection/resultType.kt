// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.filterTo
// 0 kotlin/sequences/SequencesKt.toCollection
// 0 kotlin/sequences/SequencesKt.toMutableList
// 0 kotlin/sequences/SequencesKt.toMutableSet
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.last
fun <T> firstOf(s: Sequence<T>): T = s.first()

fun box(): String {
    // the destination is of a subtype of the result type
    val filtered: MutableCollection<Int> = sequenceOf(1, 2, 3).filterTo<Int, MutableCollection<Int>>(ArrayList()) { it > 1 }
    if (filtered != listOf(2, 3)) return "Fail filterTo: $filtered"
    val collected: MutableCollection<Int> = sequenceOf(1, 2).toCollection<Int, MutableCollection<Int>>(HashSet())
    if (collected != setOf(1, 2)) return "Fail toCollection: $collected"

    val list: MutableList<Int> = sequenceOf(1, 2).toMutableList()
    list.add(3)
    if (list != listOf(1, 2, 3)) return "Fail toMutableList: $list"
    val set: MutableSet<String> = sequenceOf("a").toMutableSet()
    set.add("b")
    if (set != setOf("a", "b")) return "Fail toMutableSet: $set"

    // the result of `first`/`last` is stored in a nullable variable
    val first: Int? = sequenceOf(4, 5).first()
    if (first != 4) return "Fail first: $first"
    val last: Any = sequenceOf(4, 5).last()
    if (last != 5) return "Fail last: $last"

    // a null element is a valid result of `first` on a sequence of nullable elements
    if (sequenceOf(null, 1).first() != null) return "Fail first of nullable"
    if (firstOf(sequenceOf<Int?>(null, 1)) != null) return "Fail generic first"
    try {
        sequenceOf<Int?>().first()
        return "Fail: no exception for an empty sequence"
    } catch (e: NoSuchElementException) {
    }
    return "OK"
}
