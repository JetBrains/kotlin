// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
// a crossinline parameter called by the copied lambdas of the transformers and of the consumer
inline fun mapFiltered(items: List<Int>, crossinline keep: (Int) -> Boolean, crossinline transform: (Int) -> String): List<String> =
    sequenceOf(1, 2, 3).filter { it in items && keep(it) }.map { transform(it) }.toList()

inline fun firstMatching(crossinline predicate: (Int) -> Boolean): Int =
    generateSequence(1) { it + 1 }.first { predicate(it) }

inline fun collectEach(items: List<Int>, crossinline action: (Int) -> Unit) {
    items.asSequence().take(2).forEach { action(it) }
}

fun useInSameModule(): String = mapFiltered(listOf(1, 2, 3), { it != 2 }) { "<$it>" }.joinToString("")

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.take
// 0 kotlin/sequences/SequencesKt.forEach

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val offset = 10
    val mapped = mapFiltered(listOf(1, 2, 3), { it % 2 == 1 }) { (it + offset).toString() }
    if (mapped != listOf("11", "13")) return "Fail mapFiltered: $mapped"

    val limit = 4
    val first = firstMatching { it > limit }
    if (first != 5) return "Fail firstMatching: $first"

    val collected = mutableListOf<Int>()
    collectEach(listOf(7, 8, 9)) { collected += it }
    if (collected != listOf(7, 8)) return "Fail collectEach: $collected"

    val same = useInSameModule()
    if (same != "<1><3>") return "Fail useInSameModule: $same"
    return "OK"
}

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.take
// 0 kotlin/sequences/SequencesKt.forEach
