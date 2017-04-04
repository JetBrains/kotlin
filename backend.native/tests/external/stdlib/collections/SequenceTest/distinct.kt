import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    val sequence = fibonacci().dropWhile { it < 10 }.take(20)
    assertEquals(listOf(1, 2, 3, 0), sequence.map { it % 4 }.distinct().toList())
}
