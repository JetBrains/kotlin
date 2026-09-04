// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
class Marker(val value: Int)

// the copied lambdas use a reified type parameter, which has to be substituted at every call site
inline fun <reified T> instancesOf(items: List<Any>): List<T> =
    items.asSequence().filter { it is T }.map { it as T }.toList()

inline fun <reified T> firstInstanceOf(items: List<Any>): T =
    items.asSequence().first { it is T } as T

inline fun <reified T> countInstancesInto(items: List<Any>, destination: MutableList<Any>): MutableList<Any> =
    items.asSequence().take(3).filterTo(destination) { it is T }

// the fused code is inside a lambda of the inline function, which is regenerated at the call site
inline fun <reified T> lazyCount(items: List<Any>): () -> Int = { items.asSequence().filter { it is T }.toList().size }

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.filterTo

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val items = listOf(1, "a", Marker(2), "b")

    val strings = instancesOf<String>(items)
    if (strings != listOf("a", "b")) return "Fail instancesOf<String>: $strings"
    val ints = instancesOf<Int>(items)
    if (ints != listOf(1)) return "Fail instancesOf<Int>: $ints"

    val firstMarker = firstInstanceOf<Marker>(items)
    if (firstMarker.value != 2) return "Fail firstInstanceOf: ${firstMarker.value}"
    try {
        firstInstanceOf<Long>(items)
        return "Fail: no exception for a missing instance"
    } catch (e: NoSuchElementException) {
    }

    val destination = countInstancesInto<String>(items, mutableListOf())
    if (destination != listOf("a")) return "Fail countInstancesInto: $destination"

    val count = lazyCount<String>(items)()
    if (count != 2) return "Fail lazyCount: $count"
    return "OK"
}

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.filterTo
