import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    // find which terms are divisible by their index
    assertEquals(listOf("1/1", "5/5", "144/12", "46368/24", "75025/25"),
            fibonacci().mapIndexedNotNull { index, value ->
                if (index > 0 && (value % index) == 0) "$value/$index" else null
            }.take(5).toList())
}
