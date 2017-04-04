import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    assertEquals(listOf(2, 3, 5, 8), fibonacci().drop(3).take(4).toList())
    assertEquals(listOf(2, 3, 5, 8), fibonacci().take(7).drop(3).toList())

    val seq = fibonacci().drop(3).take(4)

    assertEquals(listOf(2, 3, 5, 8), seq.take(5).toList())
    assertEquals(listOf(2, 3, 5), seq.take(3).toList())

    assertEquals(emptyList(), seq.drop(5).toList())
    assertEquals(listOf(8), seq.drop(3).toList())

}
