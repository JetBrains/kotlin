// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
var mapped = 0

// the inline lambda parameter is called in the body of a fused for loop, so a non-local return at the call site
// has to leave the fused loop and skip the rest of the elements
inline fun forEachDoubled(items: List<Int>, action: (Int) -> Unit) {
    for (x in items.asSequence().map { mapped++; it * 2 }) action(x)
}

// the same through the `forEach` consumer, whose lambda calls the inline parameter
inline fun forEachEven(items: List<Int>, crossinline action: (Int) -> Unit) {
    items.asSequence().filter { it % 2 == 0 }.forEach { action(it) }
}

// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.forEach

// MODULE: main(lib)
// FILE: main.kt
fun findDoubled(target: Int): String {
    forEachDoubled(listOf(1, 2, 3, 4)) {
        if (it == target) return "found $it"
    }
    return "not found"
}

fun box(): String {
    val found = findDoubled(4)
    if (found != "found 4") return "Fail: $found"
    // the elements after the returned one are not mapped
    if (mapped != 2) return "Fail: mapped $mapped elements"

    mapped = 0
    val notFound = findDoubled(5)
    if (notFound != "not found") return "Fail: $notFound"
    if (mapped != 4) return "Fail: mapped $mapped elements"

    val evens = mutableListOf<Int>()
    forEachEven(listOf(1, 2, 3, 4)) { evens += it }
    if (evens != listOf(2, 4)) return "Fail forEachEven: $evens"
    return "OK"
}

// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.forEach
