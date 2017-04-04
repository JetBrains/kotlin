import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    assertEquals(listOf(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { i: Int -> i < 20 }.toList())
}
