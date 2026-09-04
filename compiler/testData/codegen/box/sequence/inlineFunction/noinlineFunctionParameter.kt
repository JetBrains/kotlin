// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
// a noinline parameter is passed directly as the predicate, so it is invoked at every place the predicate is copied to
inline fun filterThenMap(items: List<Int>, noinline keep: (Int) -> Boolean, noinline transform: (Int) -> Int): List<Int> =
    items.asSequence().filter(keep).map(transform).toList()

inline fun indexOfFirstMatching(items: List<Int>, noinline predicate: (Int) -> Boolean): Int =
    items.asSequence().indexOfFirst(predicate)

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.indexOfFirst

// MODULE: main(lib)
// FILE: main.kt
var predicateCalls = 0

fun box(): String {
    val keep: (Int) -> Boolean = { predicateCalls++; it > 1 }
    val result = filterThenMap(listOf(1, 2, 3), keep) { it * 10 }
    if (result != listOf(20, 30)) return "Fail filterThenMap: $result"
    if (predicateCalls != 3) return "Fail: predicate called $predicateCalls times"

    val index = indexOfFirstMatching(listOf(5, 6, 7), Int::isEven)
    if (index != 1) return "Fail indexOfFirstMatching: $index"
    return "OK"
}

fun Int.isEven(): Boolean = this % 2 == 0

// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.indexOfFirst
