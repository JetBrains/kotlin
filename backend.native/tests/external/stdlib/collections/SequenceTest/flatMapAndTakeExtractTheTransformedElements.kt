import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    val expected = listOf(
            '3', // fibonacci(4) = 3
            '5', // fibonacci(5) = 5
            '8', // fibonacci(6) = 8
            '1', '3', // fibonacci(7) = 13
            '2', '1', // fibonacci(8) = 21
            '3', '4', // fibonacci(9) = 34
            '5' // fibonacci(10) = 55
    )

    assertEquals(expected, fibonacci().drop(4).flatMap { it.toString().asSequence() }.take(10).toList())
}
