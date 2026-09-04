// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.generateSequence
// 0 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.sequenceOf

// MODULE: lib
// FILE: lib.kt
inline fun built(n: Int, crossinline f: (Int) -> Int): List<Int> =
    sequence { repeat(n) { yield(it) }; yieldAll(listOf(10, 11)) }.map { f(it) }.take(n + 1).toList()

inline fun firstGenerated(limit: Int): Int = generateSequence(1) { it + 1 }.filter { it > limit }.first()

inline fun <reified T> countInstances(items: List<Any>): Int {
    var c = 0
    for (x in items.asSequence().filter { it is T }) c++
    return c
}

inline fun looped(): Int {
    var acc = 0
    for (x in sequence { yield(1); yield(2) }) for (y in sequenceOf(10, 20)) acc += x * y
    return acc
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val a = built(2) { it * 3 }
    val b = firstGenerated(4)
    val c = countInstances<String>(listOf(1, "a", "b", 2.0))
    val d = looped()
    val r = "$a $b $c $d"
    return if (r == "[0, 3, 30] 5 2 90") "OK" else "Fail: $r"
}
